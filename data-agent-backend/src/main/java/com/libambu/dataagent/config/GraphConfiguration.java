package com.libambu.dataagent.config;


import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.libambu.dataagent.graph.nodes.ToyHelloNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Graph 配置，逻辑对齐 data-agent-backend-gradle 中的 GraphConfiguration.kt：
 *  START -> TOY_HELLO_NODE -> END
 */
@Configuration
public class GraphConfiguration {

    @Bean
    public StateGraph toyHelloGraph(ToyHelloNode toyHelloNode) throws Exception {
        return new StateGraph()
                .addNode("TOY_HELLO_NODE", AsyncNodeAction.node_async(toyHelloNode))
                .addEdge(StateGraph.START, "TOY_HELLO_NODE")
                .addEdge("TOY_HELLO_NODE", StateGraph.END);
    }
}
