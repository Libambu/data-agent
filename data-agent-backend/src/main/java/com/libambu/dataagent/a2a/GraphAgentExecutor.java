package com.libambu.dataagent.a2a;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskStore;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A2A Agent 执行器：把用户消息交给 Spring AI Alibaba Graph 执行，
 * 再把 Graph 的 NodeOutput 转换成 A2A Artifact 返回给客户端。
 * <p>
 * 对齐 kt 版本 {@code GraphAgentExecutor}，支持中断/恢复（人工审核）流程。
 */
@Slf4j
@Component
public class GraphAgentExecutor implements AgentExecutor {

    private final StateGraph stateGraph;
    private final TaskStore taskStore;
    private final MemorySaver saver = new MemorySaver();

    public GraphAgentExecutor(StateGraph stateGraph, TaskStore taskStore) {
        this.stateGraph = stateGraph;
        this.taskStore = taskStore;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        AtomicInteger artifactNum = new AtomicInteger();

        CompiledGraph compiledGraph;
        try {
            compiledGraph = stateGraph.compile(
                    CompileConfig.builder()
                            .saverConfig(SaverConfig.builder().register(saver).build())
                            .interruptBefore(DataAgentSpec.Graph.Node.INTERRUPT_NODE)
                            .build()
            );
        } catch (Exception e) {
            log.error("GraphAgentExecutor compile error", e);
            taskUpdater.fail();
            return;
        }

        Message message = context.getMessage();

        // 检查是否是恢复已有任务（人工审核反馈）
        String taskId = message.getTaskId();
        Task existingTask = (taskId != null) ? taskStore.get(taskId) : null;

        if (existingTask != null) {
            // 恢复已有任务：从消息元数据中获取审核结果
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(existingTask.getId()).build();
            Map<String, Object> metadata = message.getMetadata();
            Object approved = metadata != null ? metadata.get(DataAgentSpec.MessageMetadataKey.CONFIRMATION_APPROVED) : null;
            Object feedback = metadata != null ? metadata.get(DataAgentSpec.MessageMetadataKey.CONFIRMATION_FEEDBACK) : null;

            Map<String, Object> updateState = new HashMap<>();
            updateState.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED, approved);
            updateState.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, feedback);

            try {
                RunnableConfig resumedConfig = compiledGraph.updateState(runnableConfig, updateState);
                compiledGraph.stream(null, resumedConfig)
                        .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                        .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                        .blockLast();
            } catch (Exception e) {
                log.error("GraphAgentExecutor resume error", e);
                taskUpdater.fail();
            }
            return;
        }

        // 新任务：创建 Task 并开始执行
        Task newTask = newTask(message);
        eventQueue.enqueueEvent(newTask);

        TextPart textPart = (TextPart) message.getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();
        String input = textPart.getText();

        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(newTask.getId()).build();

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, input);
        Map<String, Object> metadata = message.getMetadata();
        if (metadata != null) {
            initialState.put(
                    DataAgentSpec.Graph.StateKey.Input.DATABASE_ID,
                    metadata.get(DataAgentSpec.MessageMetadataKey.DATABASE_ID)
            );
        }

        try {
            compiledGraph.stream(initialState, runnableConfig)
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                    .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                    .blockLast();
        } catch (Exception e) {
            log.error("GraphAgentExecutor execute error", e);
            taskUpdater.fail();
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        // 暂未实现
    }

    /**
     * 图执行完成后检查是否在中断节点暂停，如果是则通知客户端需要输入。
     */
    private void onComplete(CompiledGraph compiledGraph, TaskUpdater taskUpdater, RunnableConfig runnableConfig) {
        StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
        if (DataAgentSpec.Graph.Node.INTERRUPT_NODE.equals(stateSnapshot.next())) {
            taskUpdater.requiresInput();
        }
    }

    /**
     * 创建新的 Task 对象。
     */
    private Task newTask(Message message) {
        String contextId = message.getContextId();
        if (contextId == null || contextId.isEmpty()) {
            contextId = UUID.randomUUID().toString();
        }
        String id = UUID.randomUUID().toString();
        if (message.getTaskId() != null && !message.getTaskId().isEmpty()) {
            id = message.getTaskId();
        }
        return new Task(id, contextId, new TaskStatus(TaskState.SUBMITTED), null, List.of(message), null);
    }

    /**
     * 将 Graph 的 NodeOutput 转换成 A2A Artifact。
     */
    private void handleNodeOutput(NodeOutput nodeOutput,
                                  TaskUpdater taskUpdater,
                                  AtomicInteger artifactNum) {
        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
            OutputType outputType = streamingOutput.getOutputType();

            if (outputType == OutputType.GRAPH_NODE_STREAMING) {
                String streamingText = extractStreamingText(streamingOutput);
                if (streamingText == null || streamingText.isEmpty()) {
                    return;
                }
                taskUpdater.addArtifact(
                        List.<Part<?>>of(new TextPart(streamingText)),
                        String.valueOf(artifactNum.incrementAndGet()),
                        nodeOutput.node(),
                        Map.of("outputType", outputType)
                );
            } else if (outputType == OutputType.GRAPH_NODE_FINISHED) {
                taskUpdater.addArtifact(
                        List.<Part<?>>of(new DataPart(nodeOutput.state().data())),
                        String.valueOf(artifactNum.incrementAndGet()),
                        nodeOutput.node(),
                        Map.of("outputType", outputType)
                );
            }
            return;
        }
        taskUpdater.addArtifact(
                List.<Part<?>>of(new DataPart(nodeOutput.state().data())),
                String.valueOf(artifactNum.incrementAndGet()),
                nodeOutput.node(),
                Map.of()
        );
    }

    private String extractStreamingText(StreamingOutput<?> streamingOutput) {
        if (streamingOutput.message() != null) {
            return streamingOutput.message().getText();
        }
        Object originData = streamingOutput.getOriginData();
        if (originData instanceof String text) {
            return text;
        }
        return null;
    }
}
