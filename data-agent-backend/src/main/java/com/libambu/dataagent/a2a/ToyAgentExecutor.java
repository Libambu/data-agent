package com.libambu.dataagent.a2a;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * 演示版 Agent 业务执行器（核心业务入口）
 * ============================================================
 *
 * 【文件职责】
 *   实现 a2a-sdk 的 AgentExecutor 接口，定义"当一个 A2A 请求到来时，
 *   Agent 实际要做什么事"。这是整个 A2A 服务的"业务大脑"。
 *
 * 【在调用链中的位置】
 *   HTTP 请求 → A2AController → JSONRPCHandler → DefaultRequestHandler
 *               → AgentExecutor（就是本类） → 通过 EventQueue 推送结果
 *
 * 【两个核心方法】
 *   1) execute(ctx, queue) ：处理一个新请求
 *   2) cancel(ctx, queue)  ：处理任务取消（本演示版未实现）
 *
 * ============================================================
 *  ★★★ A2A 协议请求 / 响应结构速查 ★★★
 * ============================================================
 *
 * 【一、外层：JSON-RPC 2.0 信封】
 * ─────────────────────────────────────────────────────────────
 * 不管是什么 A2A 操作，外层永远是标准 JSON-RPC 2.0 格式：
 *
 *   请求体：
 *   {
 *     "jsonrpc": "2.0",
 *     "id":      "req-001",         // 客户端生成的请求 id
 *     "method":  "message/send",    // 决定请求类型，见下文枚举
 *     "params":  { ... }            // 内层业务参数，结构随 method 变化
 *   }
 *
 *   成功响应：
 *   {
 *     "jsonrpc": "2.0",
 *     "id":      "req-001",
 *     "result":  { ... }            // 业务结果对象
 *   }
 *
 *   错误响应：
 *   {
 *     "jsonrpc": "2.0",
 *     "id":      "req-001",
 *     "error":   { "code": -32601, "message": "Method not found" }
 *   }
 *
 *
 * 【二、method 枚举（决定 params/result 的形态）】
 * ─────────────────────────────────────────────────────────────
 *   非流式（HTTP 一次返回 application/json）：
 *     - message/send                              发送消息，同步等结果
 *     - tasks/get                                 查询任务状态
 *     - tasks/cancel                              取消任务
 *     - tasks/pushNotificationConfig/{set|get|list|delete}
 *     - agent/authenticatedExtendedCard           获取扩展 AgentCard
 *
 *   流式（HTTP 返回 text/event-stream 通过 SSE 推送）：
 *     - message/stream                            发送消息，流式接收事件
 *     - tasks/resubscribe                         重新订阅已存在任务的事件流
 *
 *
 * 【三、核心业务对象（params / result 内部用到）】
 * ─────────────────────────────────────────────────────────────
 *
 * ① Message（一条消息，输入输出都用它）
 *   {
 *     "kind":       "message",          // 区分对象类型的判别字段
 *     "messageId":  "msg-uuid",         // 消息唯一 id
 *     "role":       "user" | "agent",   // 谁发的
 *     "parts":      [ Part, ... ],      // 消息内容（多模态片段数组）
 *     "contextId":  "ctx-uuid",         // 会话 id（多轮对话用）
 *     "taskId":     "task-uuid",        // 关联的任务 id（可选）
 *     "metadata":         { ... },      // 自由扩展元数据
 *     "referenceTaskIds": [ "..." ],    // 引用的其它 taskId（如续写）
 *     "extensions":       [ "..." ]     // 启用的协议扩展 URI
 *   }
 *
 * ② Part（消息/产物的一个片段，多态）
 *   - TextPart：{ "kind":"text", "text":"...", "metadata":{...} }
 *   - FilePart：{ "kind":"file", "file":{ "name":"x.png", "mimeType":"...",
 *                                        "bytes":"<base64>" 或 "uri":"..." } }
 *   - DataPart：{ "kind":"data", "data":{ ...任意 JSON... } }
 *
 * ③ Task（一次 Agent 调用的"工单"，服务端维护）
 *   {
 *     "kind":      "task",
 *     "id":        "task-uuid",
 *     "contextId": "ctx-uuid",
 *     "status":    TaskStatus,          // 见 ④
 *     "artifacts": [ Artifact, ... ],   // Agent 的产出物
 *     "history":   [ Message, ... ],    // 来回消息历史
 *     "metadata":  { ... }
 *   }
 *
 * ④ TaskStatus & TaskState（任务状态）
 *   TaskStatus = { "state": TaskState, "message": Message?, "timestamp":"..." }
 *   TaskState 枚举（粗体为终态 isFinal=true）：
 *     SUBMITTED       已提交，未开始
 *     WORKING         处理中
 *     INPUT_REQUIRED  需要用户补充输入（多轮对话）
 *     AUTH_REQUIRED   需要鉴权
 *    *COMPLETED*      成功完成
 *    *CANCELED*       已取消
 *    *FAILED*         失败
 *    *REJECTED*       被拒绝
 *     UNKNOWN         未知
 *
 * ⑤ Artifact（Agent 的产出物）
 *   {
 *     "artifactId":  "1",
 *     "name":        "TOY_HELLO_NODE",  // 产物名/来源节点
 *     "description": "...",
 *     "parts":       [ Part, ... ],     // 产物内容
 *     "metadata":    { "outputType": "GRAPH_NODE_STREAMING", ... },
 *     "extensions":  [ "..." ]
 *   }
 *
 *
 * 【四、举例：message/send 的完整请求/响应】
 * ─────────────────────────────────────────────────────────────
 *
 *  ▶ 客户端 → 服务端（POST /a2a/jsonrpc）：
 *  {
 *    "jsonrpc": "2.0",
 *    "id": "req-001",
 *    "method": "message/send",
 *    "params": {
 *      "message": {
 *        "kind": "message",
 *        "messageId": "msg-001",
 *        "role": "user",
 *        "parts": [ { "kind":"text", "text":"hi" } ]
 *      },
 *      "configuration": {              // 可选
 *        "acceptedOutputModes": ["text/plain"],
 *        "blocking": true,             // true=同步等完成；false=立即返回 working
 *        "historyLength": 10
 *      }
 *    }
 *  }
 *
 *  ◀ 服务端 → 客户端（result 是一个 Task 对象）：
 *  {
 *    "jsonrpc": "2.0",
 *    "id": "req-001",
 *    "result": {
 *      "kind": "task",
 *      "id": "task-uuid",
 *      "contextId": "ctx-uuid",
 *      "status": { "state": "completed", "timestamp": "..." },
 *      "artifacts": [
 *        {
 *          "artifactId": "1",
 *          "name": "TOY_HELLO_NODE",
 *          "parts": [ { "kind":"text", "text":"hello from a2a: hi" } ],
 *          "metadata": { "outputType": "GRAPH_NODE_STREAMING" }
 *        }
 *      ],
 *      "history": [ ...原始 message... ]
 *    }
 *  }
 *
 *
 * 【五、举例：message/stream 的 SSE 事件流】
 * ─────────────────────────────────────────────────────────────
 *  请求体同 message/send（method 改为 "message/stream"）。
 *  响应 Content-Type: text/event-stream，按顺序推送多帧事件，
 *  每一帧 data: 都是一条完整的 JSON-RPC 响应：
 *
 *    data: {"jsonrpc":"2.0","id":"req-001","result":{ Task 初始 SUBMITTED }}
 *
 *    data: {"jsonrpc":"2.0","id":"req-001","result":{
 *             "kind":"status-update",
 *             "taskId":"...","status":{"state":"working"}, "final":false }}
 *
 *    data: {"jsonrpc":"2.0","id":"req-001","result":{
 *             "kind":"artifact-update",
 *             "taskId":"...", "artifact":{ ...Artifact... },
 *             "append":false, "lastChunk":true }}
 *
 *    data: {"jsonrpc":"2.0","id":"req-001","result":{
 *             "kind":"status-update",
 *             "taskId":"...","status":{"state":"completed"}, "final":true }}
 *
 *  四种 result.kind：task | status-update | artifact-update | message
 *  当 final=true 时，SSE 流结束（emitter.complete()）。
 *
 *
 * 【六、本类代码与协议的对应关系】
 * ─────────────────────────────────────────────────────────────
 *  context.getMessage()              ↔ params.message （客户端发来的 Message）
 *  context.getTaskId()/getContextId()↔ 当前 Task 的 id / contextId
 *  taskUpdater.addArtifact(...)      ↔ 推送一帧 artifact-update 事件
 *  taskUpdater.startWork()           ↔ 推 status-update(state=working)
 *  taskUpdater.complete()            ↔ 推 status-update(state=completed,final=true)
 *  taskUpdater.fail() / cancel()     ↔ 推 status-update(state=failed/canceled,final=true)
 *
 *
 * 【演示逻辑】
 *   从用户消息中取出第一个 TextPart，把里面的文本拼接成
 *   "hello from a2a: xxx"，作为输出 Artifact 返回。
 *
 * 【生产环境替换思路】
 *   把 execute() 里的逻辑替换为：
 *     - 调用 LLM（Spring AI 的 ChatClient）
 *     - 调用知识库 / RAG
 *     - 调用 Tool 工具链
 *   然后通过 TaskUpdater 把流式输出推回去即可。
 */
@Component
public class ToyAgentExecutor implements AgentExecutor {

    /**
     * 处理一次 A2A 调用。
     *
     * @param context    请求上下文，可以从中拿到原始 message、taskId、headers 等
     * @param eventQueue 事件队列，所有的输出（中间结果、最终结果）都通过它推回客户端
     */
    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        // TaskUpdater 是 SDK 提供的"任务进度更新工具"，封装了把事件
        // （如 artifact、状态变化、完成信号）正确地写入 EventQueue 的逻辑。
        TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);

        // 1. 从用户消息的多个 Part 中找到第一个文本类型的 Part。
        //    A2A 消息可以包含多种 Part：TextPart、FilePart、DataPart 等，
        //    这里只演示文本场景。
        TextPart textPart = (TextPart) context.getMessage().getParts().stream()
                .filter(p -> p instanceof TextPart)
                .findFirst()
                .orElseThrow();
        String text = textPart.getText();

        // 2. 构造一条"产物（Artifact）"推回给客户端。
        //    Artifact 表示 Agent 输出的成果物，可以是文本、文件、结构化数据等。
        //    参数说明：
        //      - parts        ：本次产物包含哪些 Part（这里只有一个 TextPart）
        //      - "1"          ：artifactId，自定义业务 id
        //      - "TOY_HELLO_NODE"：name，描述这个产物来自哪个节点（便于前端展示）
        //      - metadata     ：附加元数据，前端可据此分辨流式片段、节点类型等
        taskUpdater.addArtifact(
                List.<Part<?>>of(new TextPart("hello from a2a: " + text)),
                "1",
                "TOY_HELLO_NODE",
                Map.of("outputType", "GRAPH_NODE_STREAMING")
        );

        // 3. 通知 SDK：本次任务已经全部完成，可以把 Task 状态置为 COMPLETED
        //    并向客户端发送 final 事件。
        taskUpdater.complete();
    }

    /**
     * 处理客户端发起的取消请求。
     * 本演示版不支持取消任务（execute 是同步快速完成的），所以留空。
     * 真实场景需要：标记内部任务为已取消，停止流式输出，调用 taskUpdater.cancel()。
     */
    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        // 暂不支持取消
    }
}
