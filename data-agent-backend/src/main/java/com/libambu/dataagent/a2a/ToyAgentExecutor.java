package com.libambu.dataagent.a2a;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.libambu.dataagent.graph.ToyGraphSpec;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A2A Agent 执行器：把用户消息交给 Spring AI Alibaba Graph 执行，
 * 再把 Graph 输出转换成 A2A Artifact 返回给客户端。
 */
@Slf4j
@Component
public class ToyAgentExecutor implements AgentExecutor {

    private final StateGraph stateGraph;

    public ToyAgentExecutor(StateGraph stateGraph) {
        this.stateGraph = stateGraph;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
        AtomicInteger artifactNum = new AtomicInteger();

        TextPart textPart = (TextPart) context.getMessage().getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();
        String text = textPart.getText();

        try {
            stateGraph.compile()
                    .stream(Map.of(ToyGraphSpec.StateKey.INPUT, text))
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                    .doOnComplete(taskUpdater::complete)
                    .blockLast();
        } catch (Exception e) {
            log.error("ToyAgentExecutor execute error", e);
            taskUpdater.fail();
        }
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
