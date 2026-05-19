package com.libambu.dataagent.a2a;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
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
 * ============================================================
 * ToyAgentExecutor：A2A 请求进入后真正执行 Agent 业务的地方
 * ============================================================
 *
 * 【一句话理解】
 *   这个类负责把「A2A 协议请求」转换成「Spring AI Alibaba Graph 执行」，
 *   再把 Graph 执行过程中产生的 NodeOutput 翻译成 A2A 协议能识别的 Artifact。
 *
 * 【它在整个调用链中的位置】
 *   HTTP 请求
 *     → A2A JSON-RPC 入口
 *     → A2A SDK 的请求处理器
 *     → AgentExecutor.execute(...)，也就是本类
 *     → StateGraph.compile().stream(...)
 *     → Graph 节点执行，例如 TOY_HELLO_NODE
 *     → handleNodeOutput(...) 把 Graph 输出翻译成 A2A Artifact
 *     → TaskUpdater 通过 EventQueue 把结果推给客户端
 *
 * 【当前这份 Demo 的 Graph 链路】
 *   START → TOY_HELLO_NODE → END
 *
 *   用户输入会先被放进 Graph 初始状态：
 *     { "input": "用户输入文本" }
 *
 *   然后 ToyHelloNode 从 OverAllState 里读取 input，调用 ChatClient.stream()，
 *   大模型开始流式返回 ChatResponse，Graph 再把这些流式结果包装成 NodeOutput。
 *
 * 【A2A 协议里的几个关键对象】
 *
 *   1. Message：一条消息，客户端输入和 Agent 输出都可以是 Message。
 *      Message 里真正承载内容的是 parts 数组。
 *
 *      示例：
 *      {
 *        "kind": "message",
 *        "role": "user",
 *        "parts": [
 *          { "kind": "text", "text": "你好" }
 *        ]
 *      }
 *
 *   2. Part：消息或产物里的一个内容片段，常见类型有：
 *      - TextPart：文本片段，适合放自然语言内容，例如大模型回复。
 *      - DataPart：结构化数据片段，适合放 JSON、Map、Graph state、工具结果。
 *      - FilePart：文件片段，适合放图片、文档等文件内容。
 *
 *      可以简单记成：
 *        TextPart = 给人直接看的文本。
 *        DataPart = 给程序解析或调试查看的结构化数据。
 *
 *   3. Task：一次 Agent 调用任务。
 *      message/send 最终通常会返回一个完整 Task，Task 里面包含 status、history、artifacts。
 *
 *   4. Artifact：Agent 在任务执行过程中产生的一个“产物容器”。
 *      Artifact 不是最终内容本身，它里面的 parts 才是真正内容。
 *
 *      结构大致是：
 *      {
 *        "artifactId": "1",
 *        "name": "TOY_HELLO_NODE",
 *        "parts": [
 *          { "kind": "text", "text": "你好" }
 *        ],
 *        "metadata": {
 *          "outputType": "GRAPH_NODE_STREAMING"
 *        }
 *      }
 *
 * 【为什么本类既用 TextPart 又用 DataPart】
 *
 *   - 当 Graph 正在流式输出大模型文本时：
 *       使用 TextPart。
 *       因为这是用户要直接阅读的回答片段，例如“你好”“，我是”“AI 助手”。
 *
 *   - 当 Graph 节点执行完成，或者普通非流式节点产生输出时：
 *       使用 DataPart。
 *       因为这时更适合返回完整的 Graph state，例如：
 *       {
 *         "input": "用户问题",
 *         "output": "节点输出"
 *       }
 *
 * 【message/send 和 message/stream 的区别】
 *
 *   - message/send：
 *       非流式调用，HTTP 最后返回一个完整 Task。
 *       Artifact 会出现在 Task.artifacts 里。
 *
 *   - message/stream：
 *       流式调用，HTTP 通过 SSE 一帧一帧推送事件。
 *       每次 taskUpdater.addArtifact(...) 都会变成一帧 artifact-update 事件。
 *
 * 【本类最重要的两个方法】
 *
 *   1. execute(...)
 *      负责接收 A2A 请求、取出用户输入、启动 Graph。
 *
 *   2. handleNodeOutput(...)
 *      负责把 Graph 的 NodeOutput 翻译成 A2A 的 Artifact。
 *      这是理解 Graph 输出如何返回给 A2A 客户端的关键方法。
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
        /*
         * TaskUpdater 是 A2A SDK 提供的任务更新器。
         *
         * 你可以把它理解成“服务端往客户端推送任务进度和产物的工具”：
         *   - addArtifact(...)：推送一帧产物 artifact-update
         *   - complete()：推送任务完成 status-update
         *   - fail()：推送任务失败 status-update
         *
         * EventQueue 是底层事件队列，TaskUpdater 会把事件写到这个队列里，
         * 最终由 A2A SDK 根据 message/send 或 message/stream 的模式返回给客户端。
         */
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

        /*
         * artifactNum 用来生成 Artifact 的 artifactId。
         *
         * 每调用一次 artifactNum.incrementAndGet()，都会得到一个新的编号：
         *   1、2、3、4 ...
         *
         * 这里使用 AtomicInteger 的主要原因有两个：
         *   1. 它可以在 lambda 表达式中被安全地递增使用。
         *   2. 即使后续执行链路变成异步/多线程，也比普通 int 更稳妥。
         */
        AtomicInteger artifactNum = new AtomicInteger();

        /*
         * A2A 的 Message 由多个 Part 组成，Part 可以是 TextPart、DataPart、FilePart 等。
         *
         * 当前这个 Toy Agent 只支持文本输入，所以这里从用户消息的 parts 中找到
         * 第一个 TextPart，把它作为用户输入。
         *
         * 客户端请求里的 message 大概长这样：
         * {
         *   "kind": "message",
         *   "role": "user",
         *   "parts": [
         *     { "kind": "text", "text": "你好" }
         *   ]
         * }
         */
        TextPart textPart = (TextPart) context.getMessage().getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();

        // 真正传给 Graph 的用户文本，例如 “你好”。
        String text = textPart.getText();

        /*
         * 这里开始执行 Spring AI Alibaba Graph。
         *
         * stateGraph.compile()
         *   把 GraphConfiguration 中定义的图编译成可执行对象。
         *
         * stream(Map.of("input", text))
         *   用一个初始 state 启动图。
         *   这个 state 后续会被 ToyHelloNode 通过 state.value("input", "") 读取。
         *
         * doOnNext(...)
         *   Graph 每产生一帧 NodeOutput，就调用 handleNodeOutput(...) 翻译成 A2A Artifact。
         *
         * doOnComplete(taskUpdater::complete)
         *   Graph 执行完成后，通知 A2A 客户端任务完成。
         *
         * blockLast()
         *   阻塞当前执行流程，直到 Graph 的流式输出结束。
         */
        try {
            stateGraph.compile()
                    .stream(Map.of("input", text))
                    .doOnNext(nodeOutput -> handleNodeOutput(nodeOutput, taskUpdater, artifactNum))
                    .doOnComplete(taskUpdater::complete)
                    .blockLast();
        } catch (Exception e) {
            /*
             * 如果 Graph 编译、节点执行、模型调用或事件推送过程中出现异常，
             * 这里会记录日志，并通过 taskUpdater.fail() 通知客户端当前任务失败。
             */
            log.error("ToyAgentExecutor execute error", e);
            taskUpdater.fail();
        }
    }

    /**
     * 把一帧 Graph NodeOutput 翻译成一帧 A2A artifact-update 事件。
     *
     * 【为什么需要这个方法】
     *   Spring AI Alibaba Graph 产出的对象是 NodeOutput。
     *   A2A 客户端认识的是 Artifact / Part。
     *   所以这里要做一层“协议翻译”：
     *
     *     Graph NodeOutput
     *       → 判断它是不是 StreamingOutput
     *       → 包装成 TextPart 或 DataPart
     *       → 调用 taskUpdater.addArtifact(...)
     *       → A2A SDK 推送 artifact-update 给客户端
     *
     * 【三种输出情况】
     *
     *   1. StreamingOutput + GRAPH_NODE_STREAMING
     *      表示节点正在流式输出文本。
     *      这通常对应大模型正在一段一段吐 token。
     *      这种内容适合用 TextPart，因为前端可以直接展示。
     *
     *   2. StreamingOutput + GRAPH_NODE_FINISHED
     *      表示当前流式节点已经执行结束。
     *      这时发送 DataPart，把 nodeOutput.state().data() 作为结构化数据返回。
     *      它更像一个“节点完成后的状态快照”。
     *
     *   3. 普通 NodeOutput
     *      表示这个输出不是流式输出，通常来自普通处理节点。
     *      普通节点一般更新的是 Graph state，所以也用 DataPart 返回 state.data()。
     *
     * 【Artifact 的字段如何对应 addArtifact 参数】
     *   taskUpdater.addArtifact(parts, artifactId, name, metadata)
     *
     *   - parts：Artifact 里的内容数组，例如 TextPart 或 DataPart。
     *   - artifactId：当前产物编号，这里使用 artifactNum 自增生成。
     *   - name：产物名称，这里使用 nodeOutput.node()，也就是 Graph 节点名。
     *   - metadata：额外元数据，这里放 outputType，方便客户端区分流式中/节点结束。
     */
    private void handleNodeOutput(NodeOutput nodeOutput,
                                  TaskUpdater taskUpdater,
                                  AtomicInteger artifactNum) {
        /*
         * 第一层判断：当前 NodeOutput 是否是 StreamingOutput。
         *
         * StreamingOutput 表示“这个节点有流式输出”。
         * 在当前 Demo 中，ToyHelloNode 使用 ChatClient.stream().chatResponse()，
         * 所以大模型返回内容时会产生 StreamingOutput。
         *
         * Java 这里使用的是 instanceof 模式匹配写法：
         *   如果 nodeOutput 是 StreamingOutput 类型，
         *   就直接把它转换为 streamingOutput 变量使用。
         */
        if (nodeOutput instanceof StreamingOutput<?> streamingOutput) {
            /*
             * outputType 表示当前流式输出处于哪个阶段。
             *
             * 当前重点关注两类：
             *   - GRAPH_NODE_STREAMING：节点正在流式输出文本片段。
             *   - GRAPH_NODE_FINISHED：节点流式输出已经结束。
             */
            OutputType outputType = streamingOutput.getOutputType();

            if (outputType == OutputType.GRAPH_NODE_STREAMING) {
                /*
                 * 分支一：节点正在流式输出文本。
                 *
                 * streamingOutput.message().getText() 是当前这一帧的文本内容，
                 * 例如模型可能依次返回：
                 *   “你好”
                 *   “，我是”
                 *   “AI 助手”
                 *
                 * 这些内容是给用户直接看的自然语言，所以包装成 TextPart。
                 *
                 * 最终发给客户端的 Artifact 大致类似：
                 * {
                 *   "artifactId": "1",
                 *   "name": "TOY_HELLO_NODE",
                 *   "parts": [
                 *     { "kind": "text", "text": "你好" }
                 *   ],
                 *   "metadata": {
                 *     "outputType": "GRAPH_NODE_STREAMING"
                 *   }
                 * }
                 */
                taskUpdater.addArtifact(
                        List.<Part<?>>of(new TextPart(streamingOutput.message().getText())),
                        String.valueOf(artifactNum.incrementAndGet()),
                        nodeOutput.node(),
                        Map.of("outputType", outputType)
                );
            } else if (outputType == OutputType.GRAPH_NODE_FINISHED) {
                /*
                 * 分支二：流式节点已经执行结束。
                 *
                 * 这时不再发送某个 token 文本，而是发送当前 Graph 的完整状态快照。
                 * nodeOutput.state().data() 通常可以理解为当前 OverAllState 里的 Map 数据。
                 *
                 * 结构化 state 更适合用 DataPart 表示，因为它不是单纯给人读的一句话，
                 * 而是给前端、调试工具或后续程序解析的 JSON 数据。
                 *
                 * 最终发给客户端的 Artifact 大致类似：
                 * {
                 *   "artifactId": "2",
                 *   "name": "TOY_HELLO_NODE",
                 *   "parts": [
                 *     {
                 *       "kind": "data",
                 *       "data": {
                 *         "input": "用户问题",
                 *         "output": "节点输出或状态数据"
                 *       }
                 *     }
                 *   ],
                 *   "metadata": {
                 *     "outputType": "GRAPH_NODE_FINISHED"
                 *   }
                 * }
                 */
                taskUpdater.addArtifact(
                        List.<Part<?>>of(new DataPart(nodeOutput.state().data())),
                        String.valueOf(artifactNum.incrementAndGet()),
                        nodeOutput.node(),
                        Map.of("outputType", outputType)
                );
            }
        } else {
            /*
             * 分支三：普通 NodeOutput。
             *
             * 如果某个 Graph 节点不是流式节点，例如：
             *   - 预处理节点 PREPROCESS_NODE
             *   - 检索节点 RETRIEVE_NODE
             *   - 普通工具调用节点 TOOL_NODE
             *
             * 它通常不会一段一段输出文本，而是一次性更新 Graph state。
             * 所以这里同样用 DataPart 返回 nodeOutput.state().data()。
             *
             * metadata 传 Map.of()，表示没有额外的 outputType 信息。
             */
            taskUpdater.addArtifact(
                    List.<Part<?>>of(new DataPart(nodeOutput.state().data())),
                    String.valueOf(artifactNum.incrementAndGet()),
                    nodeOutput.node(),
                    Map.of()
            );
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        // 与 gradle 版保持一致：暂未实现
    }
}
