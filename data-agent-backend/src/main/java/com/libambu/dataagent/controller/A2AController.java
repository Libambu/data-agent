package com.libambu.dataagent.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.common.A2AHeaders;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.NonStreamingJSONRPCRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.a2a.util.Utils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.FlowAdapters;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;

/**
 * ============================================================
 * A2A 协议 HTTP 入口 Controller
 * ============================================================
 *
 * 【文件职责】
 *   作为 A2A 服务对外的 HTTP 入口，把 HTTP/JSON-RPC 请求翻译成
 *   对 JSONRPCHandler 的方法调用，再把结果按 JSON 或 SSE 格式回写给客户端。
 *
 * 【两个对外接口】
 *   1) GET  /.well-known/agent-card.json  →  返回 AgentCard（Agent 名片）
 *   2) POST /a2a/jsonrpc                  →  接收所有 JSON-RPC 请求
 *
 * 【handleRequest() 的整体流程】
 *
 *      原始 JSON 字符串 body
 *              │
 *              ▼
 *      解析为 JsonNode → 取出 method 字段
 *              │
 *      ┌───────┴───────┐
 *      ▼               ▼
 *   流式请求         非流式请求
 *  (message/stream,   (message/send,
 *   tasks/resubscribe) tasks/get,
 *      │               tasks/cancel ...)
 *      │                       │
 *      ▼                       ▼
 *   processStreamingRequest   processNonStreamingRequest
 *      │                       │
 *      ▼                       ▼
 *   返回 Flux              返回单个 JSONRPCResponse
 *      │                       │
 *      ▼                       ▼
 *   SseEmitter（SSE 流）    ResponseEntity（JSON）
 *
 * 【为什么要区分流式/非流式】
 *   - 流式方法返回 Flow.Publisher，需要把每一帧通过 SSE 实时推送给客户端
 *   - 非流式方法直接返回单个对象，HTTP 响应一次性返回 JSON 即可
 *
 * 【对应原 Kotlin 实现】
 *   data-agent-backend 中的 A2AController.kt（用 when 表达式分发请求），
 *   这里改写为 Java 17 的 instanceof 模式匹配。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class A2AController {

    /** 用于把当前 HTTP 请求的 method 名（如 "message/send"）放到 ServerCallContext 的 state 里，便于下游识别 */
    private static final String METHOD_NAME_KEY = "methodName";
    /** 用于把当前 HTTP 请求所有 header 放到 ServerCallContext 的 state 里，便于下游读取（如鉴权 token） */
    private static final String HEADERS_KEY = "headers";

    /** 当前 HTTP 请求对象（Spring 会注入 request-scoped 代理），用来取 header */
    private final HttpServletRequest request;
    /** A2A JSON-RPC 处理器，由 A2AConfiguration#jsonRpcHandler 装配 */
    private final JSONRPCHandler jsonRpcHandler;

    @PostConstruct
    public void init() {
        // 预留：如需注册自定义 Jackson 模块，可在此扩展
    }

    /**
     * Agent 名片接口。
     * A2A 客户端会先请求这个接口，从中读取 Agent 的能力、URL、协议等信息，
     * 之后才知道要把 JSON-RPC 请求发到哪里。
     */
    @GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentCard agentJson() {
        return jsonRpcHandler.getAgentCard();
    }

    /**
     * A2A 协议主入口：接收所有 JSON-RPC 请求。
     *这个方法可能返回 JSON（普通响应），也可能返回 SSE 流（SseEmitter），两种类型不一样，所以只能用 Object。
     * 同时支持两种返回类型：
     *   - text/event-stream（SSE）：用于 streaming 请求
     *   - application/json：用于普通同步请求
     *
     * @param body 原始 JSON 字符串（不直接绑定为 POJO，因为要先看 method 字段才能确定具体类型）
     * @return ResponseEntity（普通请求）或 SseEmitter（流式请求）
     */
    @PostMapping(
            value = "/a2a/jsonrpc",
            produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    public Object handleRequest(@RequestBody String body) {
        boolean streaming = false;
        // 构建 ServerCallContext：把 user / headers / 扩展信息打包成上下文，往下游传
        ServerCallContext context = createCallContext();
        JSONRPCResponse<?> nonStreamingResponse = null;
        Flux<? extends JSONRPCResponse<?>> streamingResponse = null;
        JSONRPCErrorResponse error = null;

        try {
            // 1) 先把 body 解析成 JsonNode，根据 method 字段判断是不是流式
            JsonNode node = Utils.OBJECT_MAPPER.readTree(body);
            JsonNode method = node == null ? null : node.get("method");
            streaming = method != null && (
                    SendStreamingMessageRequest.METHOD.equals(method.asText())
                            || TaskResubscriptionRequest.METHOD.equals(method.asText())
            );
            // 把 method 名放进 context.state，下游业务代码（如审计、限流）可以读取
            String methodName = (method != null && method.isTextual()) ? method.asText() : null;
            if (methodName != null) {
                context.getState().put(METHOD_NAME_KEY, methodName);
            }

            // 2) 根据 streaming 把 JsonNode 反序列化成对应的请求 POJO
            if (streaming) {
                StreamingJSONRPCRequest<?> req = Utils.OBJECT_MAPPER.treeToValue(node, StreamingJSONRPCRequest.class);
                streamingResponse = processStreamingRequest(req, context);
            } else {
                NonStreamingJSONRPCRequest<?> req = Utils.OBJECT_MAPPER.treeToValue(node, NonStreamingJSONRPCRequest.class);
                nonStreamingResponse = processNonStreamingRequest(req, context);
            }
        } catch (JsonProcessingException e) {
            // JSON 解析/反序列化错误 → 返回标准的 JSON-RPC 错误响应
            error = handleError(e);
        } catch (Throwable t) {
            // 兜底：任何其他异常都封装成 InternalError 返回，避免 500
            error = new JSONRPCErrorResponse(new InternalError(t.getMessage()));
        }

        // 3) 出错 → 直接返回 JSON 错误体
        if (error != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Utils.toJsonString(error));
        }

        // 4) 流式请求 → 用 SseEmitter 把每个事件序列化后通过 SSE 推送给客户端
        if (streaming) {
            // timeout 设为 0 表示不主动断开，由业务侧决定何时 complete
            SseEmitter emitter = new SseEmitter(0L);
            Flux<? extends JSONRPCResponse<?>> flux = streamingResponse != null
                    ? streamingResponse
                    : Flux.just(new JSONRPCErrorResponse(new InternalError("Streaming response is null")));
            flux.subscribe(
                    // onNext：每收到一个事件就 send 一次 SSE
                    response -> {
                        try {
                            emitter.send(SseEmitter.event().data(Utils.toJsonString(response)));
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }
                    },
                    // onError：流出现异常 → 让 SSE 也带异常结束
                    e -> {
                        log.error("SSE transport failed", e);
                        emitter.completeWithError(e);
                    },
                    // onComplete：流结束 → 关闭 SSE
                    emitter::complete
            );
            return emitter;
        }

        // 5) 非流式请求 → 直接 JSON 序列化返回
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Utils.toJsonString(nonStreamingResponse));
    }

    /**
     * 把 Jackson 抛出的各种 JSON 错误，映射成 A2A/JSON-RPC 标准错误码。
     *   JsonParseException / JsonEOFException → JSONParseError（-32700）
     *   MethodNotFoundJsonMappingException    → MethodNotFoundError（-32601）
     *   InvalidParamsJsonMappingException     → InvalidParamsError（-32602）
     *   IdJsonMappingException / 其它          → InvalidRequestError（-32600）
     * 同时尽可能从异常中提取出原始请求 id 一起回传，便于客户端关联。
     */
    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
        Object id = null;
        JSONRPCError jsonRpcError;
        if (exception.getCause() instanceof JsonParseException) {
            jsonRpcError = new JSONParseError();
        } else if (exception instanceof JsonEOFException) {
            jsonRpcError = new JSONParseError(exception.getMessage());
        } else if (exception instanceof MethodNotFoundJsonMappingException ex) {
            id = ex.getId();
            jsonRpcError = new MethodNotFoundError();
        } else if (exception instanceof InvalidParamsJsonMappingException ex) {
            id = ex.getId();
            jsonRpcError = new InvalidParamsError();
        } else if (exception instanceof IdJsonMappingException ex) {
            id = ex.getId();
            jsonRpcError = new InvalidRequestError();
        } else {
            jsonRpcError = new InvalidRequestError();
        }
        return new JSONRPCErrorResponse(id, jsonRpcError);
    }

    /**
     * 非流式请求路由：根据 JSON-RPC 请求的具体子类型（method）调用对应的 handler 方法。
     * 这里 instanceof 模式匹配等价于 Kotlin 的 when (request) is Xxx -> ...。
     *
     * 主要 method 与方法的映射：
     *   tasks/get                              → onGetTask
     *   tasks/cancel                           → onCancelTask
     *   tasks/pushNotificationConfig/set       → setPushNotificationConfig
     *   tasks/pushNotificationConfig/get       → getPushNotificationConfig
     *   tasks/pushNotificationConfig/list      → listPushNotificationConfig
     *   tasks/pushNotificationConfig/delete    → deletePushNotificationConfig
     *   message/send                           → onMessageSend
     *   agent/authenticatedExtendedCard        → onGetAuthenticatedExtendedCardRequest
     */
    private JSONRPCResponse<?> processNonStreamingRequest(
            NonStreamingJSONRPCRequest<?> request,
            ServerCallContext context
    ) {
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else if (request instanceof GetAuthenticatedExtendedCardRequest req) {
            return jsonRpcHandler.onGetAuthenticatedExtendedCardRequest(req, context);
        }
        // 未识别的 method → 返回 Unsupported
        return generateErrorResponse(request, new UnsupportedOperationError());
    }

    /**
     * 流式请求路由：仅处理两种 method
     *   message/stream         → onMessageSendStream
     *   tasks/resubscribe      → onResubscribeToTask
     *
     * SDK 用 java.util.concurrent.Flow.Publisher，
     * 这里通过 FlowAdapters 转换为 Reactor Flux，方便后续接 SseEmitter。
     */
    private Flux<? extends JSONRPCResponse<?>> processStreamingRequest(
            JSONRPCRequest<?> request,
            ServerCallContext context
    ) {
        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest req) {
            publisher = jsonRpcHandler.onMessageSendStream(req, context);
        } else if (request instanceof TaskResubscriptionRequest req) {
            publisher = jsonRpcHandler.onResubscribeToTask(req, context);
        } else {
            return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()));
        }
        return Flux.from(FlowAdapters.toPublisher(publisher));
    }

    /** 生成"携带原请求 id"的错误响应 */
    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

    /**
     * 构造 ServerCallContext：a2a-sdk 的"调用上下文"。
     * 它会被一直传递到 AgentExecutor，业务里可以用它读取调用方信息：
     *   - user：调用者身份（这里演示版用未鉴权用户）
     *   - state：自由扩展的 KV，存放 method 名、headers 等
     *   - requestedExtensions：客户端通过 X-A2A-Extensions header 请求启用的扩展
     */
    private ServerCallContext createCallContext() {
        // 未鉴权用户。生产场景应从 SecurityContext / JWT 中解析出真实 User
        UnauthenticatedUser user = UnauthenticatedUser.INSTANCE;
        Map<String, Object> state = new HashMap<>();

        // 1) 收集所有 HTTP header 放进 state，便于下游业务访问
        Map<String, String> requestHeaders = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            requestHeaders.put(name, request.getHeader(name));
        }
        state.put(HEADERS_KEY, requestHeaders);

        // 2) 解析 X-A2A-Extensions header（可能多个值），交给 SDK 工具方法
        //    转成它需要的 Set<String> 形式
        List<String> extensionHeaderValues = new ArrayList<>();
        Enumeration<String> extHeaders = request.getHeaders(A2AHeaders.X_A2A_EXTENSIONS);
        if (extHeaders != null) {
            while (extHeaders.hasMoreElements()) {
                extensionHeaderValues.add(extHeaders.nextElement());
            }
        }
        Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

        return new ServerCallContext(user, state, requestedExtensions);
    }
}
