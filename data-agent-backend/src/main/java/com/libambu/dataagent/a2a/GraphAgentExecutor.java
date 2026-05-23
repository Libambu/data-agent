package com.libambu.dataagent.a2a;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A2A Agent 执行器：把用户消息交给 Spring AI Alibaba Graph 执行，
 * 再把 Graph 的 NodeOutput 转换成 A2A Artifact 返回给客户端。
 * <p>
 * 对齐 kt 版本 {@code GraphAgentExecutor}，仅做最小可运行编排，
 * 不再处理多场景路由 / 中断恢复等 Toy 流程。
 */
@Slf4j
@Component
public class GraphAgentExecutor implements AgentExecutor {

    private final StateGraph stateGraph;

    public GraphAgentExecutor(StateGraph stateGraph) {
        this.stateGraph = stateGraph;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        AtomicInteger artifactNum = new AtomicInteger();

        Message message = context.getMessage();
        TextPart textPart = (TextPart) message.getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();
        String input = textPart.getText();

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, input);
        initialState.put(
                DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT,
                buildMultiTurnContext(context, message)
        );
        Map<String, Object> metadata = message.getMetadata();
        if (metadata != null) {
            initialState.put(
                    DataAgentSpec.Graph.StateKey.Input.DATABASE_ID,
                    metadata.get(DataAgentSpec.MessageMetadataKey.DATABASE_ID)
            );
        }

        try {
            stateGraph.compile()
                    .stream(initialState)
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                    .doOnComplete(taskUpdater::complete)
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

    private String buildMultiTurnContext(RequestContext context, Message currentMessage) {
        Task task = context.getTask();
        if (task == null || task.getHistory() == null || task.getHistory().isEmpty()) {
            return "(无)";
        }

        String currentMessageId = currentMessage == null ? null : currentMessage.getMessageId();
        List<String> historyLines = task.getHistory().stream()
                .filter(message -> message != null)
                .filter(message -> currentMessageId == null || !currentMessageId.equals(message.getMessageId()))
                .map(this::formatHistoryMessage)
                .filter(line -> !line.isBlank())
                .toList();

        if (historyLines.isEmpty()) {
            return "(无)";
        }
        return String.join("\n", historyLines);
    }

    private String formatHistoryMessage(Message message) {
        String content = extractText(message);
        if (content.isBlank()) {
            return "";
        }
        return roleLabel(message) + ": " + content;
    }

    private String extractText(Message message) {
        if (message == null || message.getParts() == null) {
            return "";
        }
        return message.getParts().stream()
                .filter(part -> part instanceof TextPart)
                .map(part -> ((TextPart) part).getText())
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private String roleLabel(Message message) {
        if (message == null || message.getRole() == null) {
            return "未知";
        }
        return switch (message.getRole()) {
            case USER -> "用户";
            case AGENT -> "AI";
        };
    }
}
