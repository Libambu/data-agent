package com.libambu.dataagent.a2a;

import io.a2a.server.TransportMetadata;
import io.a2a.spec.TransportProtocol;

/**
 * ============================================================
 * A2A 传输协议元数据声明（JSON-RPC 通道）
 * 
 * ============================================================
 *
 * 【文件职责】
 *   告诉 a2a-sdk 框架："本 Agent 服务支持通过 JSON-RPC 协议进行通信"。
 *
 * 【为什么需要它】
 *   A2A（Agent-to-Agent）协议本身是抽象的传输无关协议，它可以跑在多种
 *   通信方式之上（JSON-RPC over HTTP、gRPC、REST 等）。SDK 需要在启动
 *   时知道当前服务器实现了哪些传输方式，才能正确地把请求路由进来。
 *
 * 【它如何被发现】
 *   通过 Java SPI（Service Provider Interface）机制：
 *     resources/META-INF/services/io.a2a.server.TransportMetadata
 *   该文件中写了本类的全限定名，SDK 会通过 ServiceLoader 自动加载它。
 *
 * 【对应关系】
 *   AgentCard 中的 url、additionalInterfaces 中声明的 "JSONRPC" 类型，
 *   就是依赖本类返回的 TransportProtocol.JSONRPC 来对应的。
 *
 * 【实现要点】
 *   只需重写 getTransportProtocol() 返回固定字符串 "JSONRPC" 即可。
 */
public class JSONRPCTransportMetadata implements TransportMetadata {

    /**
     * 返回当前实现的传输协议类型字符串。
     * TransportProtocol.JSONRPC 是 SDK 预定义的枚举/常量，对应 "JSONRPC"。
     */
    @Override
    public String getTransportProtocol() {
        return TransportProtocol.JSONRPC.toString();
    }
}
