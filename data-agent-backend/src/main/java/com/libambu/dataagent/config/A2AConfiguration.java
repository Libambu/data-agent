package com.libambu.dataagent.config;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ============================================================
 * A2A 服务的 Spring 装配中心
 * ============================================================
 *
 * 【文件职责】
 *   把 a2a-sdk 运行所需的全部组件（AgentCard、TaskStore、QueueManager、
 *   PushNotification、JSONRPCHandler 等）装配成 Spring Bean，从而被
 *   A2AController 和业务代码注入使用。
 *
 * 【组件依赖关系图】
 *
 *     AgentCard ───────────────┐
 *                              ▼
 *                       JSONRPCHandler
 *                              ▲
 *     AgentExecutor ─┐         │
 *     TaskStore ─────┤         │
 *     QueueManager ──┼─→ DefaultRequestHandler
 *     PushCfgStore ──┤
 *     PushSender ────┘
 *
 *   - AgentCard：对外名片，描述 Agent 的能力、技能、URL、协议
 *   - TaskStore：保存任务（Task）状态的存储（这里用内存版）
 *   - QueueManager：管理每个任务的事件流队列
 *   - PushNotification 系列：负责异步推送通知给客户端
 *   - DefaultRequestHandler：把上面所有组件粘起来，提供高层 API
 *   - JSONRPCHandler：在 DefaultRequestHandler 之上封装出
 *     "JSON-RPC 方法名 → 处理函数" 的映射，A2AController 直接调用它
 */
@Configuration
public class A2AConfiguration {

    /**
     * Agent 名片：对外暴露的"自我介绍"，A2A 客户端通过
     *   GET /.well-known/agent-card.json
     * 拉取本对象，从而知道这个 Agent 叫什么、能做什么、走哪种协议、URL 是什么。
     */
    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
                // Agent 名称
                .name("sqlAgent")
                // Agent 描述（人类可读）
                .description("专业的SQL生成Agent")
                // 默认接收的内容类型
                .defaultInputModes(List.of("text/plain"))
                // 默认输出的内容类型
                .defaultOutputModes(List.of("text/plain"))
                // 能力声明：是否支持流式、推送通知、状态历史等
                .capabilities(
                        new AgentCapabilities.Builder()
                                .streaming(true)              // 支持 SSE 流式输出
                                .pushNotifications(true)      // 支持 webhook 推送
                                .stateTransitionHistory(true) // 支持查询任务状态变更历史
                                .build()
                )
                // 技能列表：Agent 提供哪些"能干的活"
                .skills(
                        List.of(
                                new AgentSkill.Builder()
                                        .id("sql")
                                        .name("sql generator")
                                        .description("Generate a SQL query")
                                        .tags(List.of("sql", "query"))
                                        .build()
                        )
                )
                // 主调用 URL（必填）：客户端会把 JSON-RPC 请求 POST 到这里
                .url("http://localhost:3500/api/a2a/jsonrpc")
                // 附加接口：声明本 Agent 还支持哪些传输方式 + 各自的 URL
                .additionalInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://localhost:3500/api/a2a/jsonrpc")
                ))
                // 版本号
                .version("1.0")
                .build();
    }

    /**
     * 任务存储：把每一次 A2A 调用产生的 Task 状态记录下来。
     * 这里用内存版，重启即丢失。生产环境可替换为 PG/Redis 实现。
     */
    @Bean
    public InMemoryTaskStore taskStore() {
        return new InMemoryTaskStore();
    }

    /**
     * Push Notification 配置存储：保存"客户端要求把进度推送到哪个 webhook URL"。
     * 同样是内存版。
     */
    @Bean
    public InMemoryPushNotificationConfigStore pushNotificationConfigStore() {
        return new InMemoryPushNotificationConfigStore();
    }

    /**
     * Push Notification 发送器：基于上面的配置，把任务进度真正 POST 出去。
     */
    @Bean
    public BasePushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
        return new BasePushNotificationSender(pushNotificationConfigStore);
    }

    /**
     * 队列管理器：为每个 taskId 维护一个事件队列（EventQueue），
     * AgentExecutor 往里写事件，订阅者（SSE 流）从里面读事件。
     */
    @Bean
    public QueueManager queueManager(InMemoryTaskStore taskStore) {
        return new InMemoryQueueManager(taskStore);
    }

    /**
     * 阻塞模式下等待 Agent 完成的最大时间（秒）。
     * 当客户端使用同步（非流式）调用时，超过这个时间就返回超时。
     */
    @Bean
    public Integer agentCompletionTimeoutSeconds(
            @Value("${a2a.blocking.agent.timeout.seconds:30}") Integer timeout
    ) {
        return timeout;
    }

    /**
     * JSON-RPC 处理器（最终被 A2AController 注入使用）。
     * 它把"原始 JSON-RPC 请求"翻译成对 DefaultRequestHandler 中具体方法的调用，
     * 例如 "message/send" → handler.onMessageSend(...)。
     *
     * 内部用一个固定 5 线程的线程池处理并发任务（演示规模够用）。
     */
    @Bean
    public JSONRPCHandler jsonRpcHandler(
            AgentCard agentCard,
            AgentExecutor agentExecutor,                     // 由 ToyAgentExecutor 注入
            TaskStore taskStore,
            QueueManager queueManager,
            PushNotificationConfigStore pushNotificationConfigStore,
            PushNotificationSender pushNotificationSender
    ) {
        // 业务执行用的线程池
        ExecutorService pool = Executors.newFixedThreadPool(5);

        // DefaultRequestHandler 是 SDK 默认实现，把所有组件粘合起来
        DefaultRequestHandler handler = DefaultRequestHandler.create(
                agentExecutor,
                taskStore,
                queueManager,
                pushNotificationConfigStore,
                pushNotificationSender,
                pool
        );

        // 在 handler 外面再包一层 JSONRPCHandler，提供 JSON-RPC 风格 API
        return new JSONRPCHandler(agentCard, handler, pool);
    }
}
