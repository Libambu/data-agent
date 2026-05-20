package com.libambu.dataagent.a2a;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.libambu.dataagent.graph.ToyGraphSpec;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A2A Agent 执行器：把用户消息交给 Spring AI Alibaba Graph 执行，
 * 再把 Graph 输出转换成 A2A Artifact 返回给客户端。
 */
@Slf4j
@Component
public class ToyAgentExecutor implements AgentExecutor {

    private final StateGraph stateGraph;
    private final TaskStore taskStore;
    private final MemorySaver saver = new MemorySaver();

    public ToyAgentExecutor(StateGraph stateGraph, TaskStore taskStore) {
        this.stateGraph = stateGraph;
        this.taskStore = taskStore;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        AtomicInteger artifactNum = new AtomicInteger();
        Message message = context.getMessage();

        try {
            CompiledGraph compiledGraph = stateGraph.compile(
                    CompileConfig.builder()
                            //注册内存型 Saver，保存 Graph 状态到内存中，方便后续查询和恢复
                            .saverConfig(SaverConfig.builder().register(saver).build())
                            //执行到 CONFIRM_NODE 前，先暂停,START -> ROUTE -> 停在 CONFIRM_NODE 前 
                            .interruptBefore(ToyGraphSpec.Node.INTERRUPT_NODE)
                            .build()
            );

            Task existingTask = findExistingTask(message);
            if (existingTask != null) {
                resumeExistingTask(message, compiledGraph, taskUpdater, artifactNum, existingTask);
                return;
            }

            startNewTask(message, eventQueue, compiledGraph, taskUpdater, artifactNum);
        } catch (Exception e) {
            log.error("ToyAgentExecutor execute error", e);
            taskUpdater.fail();
        }
    }

    /**
     * {
            "id": "任务ID",
            "contextId": "上下文ID",
            "status": {
                "state": "INPUT_REQUIRED"
            },
            "artifacts": ["之前已经产生的输出"],
            "history": ["之前的用户消息"],
            "metadata": {}
        }
     * @param message
     * @return
     */
    private Task findExistingTask(Message message) {
        String taskId = message.getTaskId();
        if (taskId == null || taskId.isEmpty()) {
            return null;
        }
        return taskStore.get(taskId);
    }

    private void resumeExistingTask(Message message,
                                    CompiledGraph compiledGraph,
                                    TaskUpdater taskUpdater,
                                    AtomicInteger artifactNum,
                                    Task existingTask) throws Exception {
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(existingTask.getId())
                .build();
        RunnableConfig resumedConfig = compiledGraph.updateState(
                runnableConfig,
                Map.of(
                        ToyGraphSpec.StateKey.CONFIRMATION_APPROVED, confirmationApproved(message),
                        ToyGraphSpec.StateKey.CONFIRMATION_FEEDBACK, confirmationFeedback(message)
                )
        );

        compiledGraph.stream(null, resumedConfig)
                .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                .blockLast();
    }

    private void startNewTask(Message message,
                              EventQueue eventQueue,
                              CompiledGraph compiledGraph,
                              TaskUpdater taskUpdater,
                              AtomicInteger artifactNum) {
        Task newTask = newTask(message);
        if (eventQueue != null) {
            //向客户端推送，告诉它这个 Task 已经创建了，后续的 Graph 执行结果会关联到这个 Task 上
            eventQueue.enqueueEvent(newTask);
        }

        TextPart textPart = (TextPart) message.getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();
        String text = textPart.getText();

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId(newTask.getId())
                .build();

        //编译图启动了MemorySaver 会保存 Graph 的执行状态。而保存和恢复状态，需要一个标识。这里我们用 Task 的 id 作为 Graph 执行的 threadId，
        // 这样就能通过 MemorySaver 根据 threadId 来保存和查询状态了。
        compiledGraph.stream(Map.of(ToyGraphSpec.StateKey.INPUT, text), runnableConfig)
                //把节点的输出发给前端
                .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                .doOnComplete(() -> onComplete(compiledGraph, taskUpdater, runnableConfig))
                .blockLast();
    }

/**
 * 这个 Task 一方面会通知客户端任务已创建，另一方面它的 id 会作为 Graph 的 threadId，用于保存、暂停和恢复这次 Agent 执行。
 * @param request
 * @return
 * 
 * request结构
    {
        "contextId": null,
        "taskId": null,
        "parts": [
            {
            "kind": "text",
            "text": "帮我查询今天的销售额"
            }
        ],
        "metadata": {}
    } 
    返回的Task结构
    {
        "id": "生成的-task-uuid",
        "contextId": "生成的-context-uuid",
        "status": {
            "state": "SUBMITTED"
        },
        "artifacts": null,
        "history": [
            {
            "parts": [
                {
                "kind": "text",
                "text": "帮我查询今天的销售额"
                }
            ]
            }
        ],
        "metadata": null
    }
 */
    private Task newTask(Message request) {
        String contextId = request.getContextId();
        if (contextId == null || contextId.isEmpty()) {
            contextId = UUID.randomUUID().toString();
        }
        String id = UUID.randomUUID().toString();
        if (request.getTaskId() != null && !request.getTaskId().isEmpty()) {
            id = request.getTaskId();
        }
        return new Task(id, contextId, new TaskStatus(TaskState.SUBMITTED), null, List.of(request), null);
    }

    private void onComplete(CompiledGraph compiledGraph,
                            TaskUpdater taskUpdater,
                            RunnableConfig runnableConfig) {
        //拿到当前 Graph 的状态快照。如果 Graph 已经执行完了，那么状态快照的 next node 就是 null；如果 Graph 在某个节点暂停了，那么状态快照的 next node 就是那个节点。
        StateSnapshot stateSnapshot = compiledGraph.getState(runnableConfig);
        if (ToyGraphSpec.Node.INTERRUPT_NODE.equals(stateSnapshot.next())) {
            //给用户返回一天信息
            /**
             * {
                    "kind": "status-update",
                    "taskId": "xxx",
                    "status": {
                        "state": "INPUT_REQUIRED"
                    }
                }
             */
            //下次用户的消息就会有
            // "metadata": {
            //     "confirmationApproved": false,
            //     "confirmationFeedback": "行程太赶了，减少景点"
            // } 
            taskUpdater.requiresInput();
            return;
        }
        taskUpdater.complete();
    }

    private Boolean confirmationApproved(Message message) {
        Object value = message.getMetadata().get(ToyGraphSpec.MessageMetadataKey.CONFIRMATION_APPROVED);
        if (value instanceof Boolean approved) {
            return approved;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private String confirmationFeedback(Message message) {
        Object value = message.getMetadata().get(ToyGraphSpec.MessageMetadataKey.CONFIRMATION_FEEDBACK);
        return value == null ? "" : value.toString();
    }

    /**
     * 将 Graph 的 NodeOutput 转换成 A2A Artifact。
     */
    /**
     * 
     * @param nodeOutput
     * @param taskUpdater
     * @param artifactNum
     * {
            "kind": "artifact-update",
            "artifact": {
                "artifactId": "1",
                "name": "TRAVEL_PLAN_NODE",
                "parts": [
                {
                    "kind": "text",
                    "text": "第一天上午可以去西湖..."
                }
                ],
                "metadata": {
                "outputType": "GRAPH_NODE_STREAMING"
                }
            }
        }
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
        } else {
            taskUpdater.addArtifact(
                    List.<Part<?>>of(new DataPart(nodeOutput.state().data())),
                    String.valueOf(artifactNum.incrementAndGet()),
                    nodeOutput.node(),
                    Map.of()
            );
        }
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

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        // 暂未实现
    }
}
