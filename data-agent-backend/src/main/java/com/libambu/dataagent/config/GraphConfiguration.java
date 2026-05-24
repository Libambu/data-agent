package com.libambu.dataagent.config;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.libambu.dataagent.agent.nodes.EvidenceRecallNode;
import com.libambu.dataagent.agent.nodes.SchemeReCallNode;
import com.libambu.dataagent.agent.nodes.TableRelationNode;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Agent 图配置。
 */
@Configuration
public class GraphConfiguration {

    /**
     * 数据 Agent 主链路 Graph：
     * START -> EVIDENCE_RECALL_NODE -> SCHEME_RECALL_NODE -> TABLE_RELATION_NODE -> END
     * <p>
     * 当前对齐 kt 版的最小可运行编排，后续会接入 PLANNER / SQL / Python 等节点。
     */
    @Bean
    public StateGraph dataAgentMainGraph(EvidenceRecallNode evidenceRecallNode,
                                         SchemeReCallNode schemeReCallNode,
                                         TableRelationNode tableRelationNode) throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> map = new HashMap<>();
            map.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, KeyStrategy.REPLACE);
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, KeyStrategy.REPLACE);
            return map;
        };

        return new StateGraph(DataAgentSpec.GRAPH_NAME, keyStrategyFactory)
                .addNode(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, AsyncNodeAction.node_async(evidenceRecallNode))
                .addNode(DataAgentSpec.Graph.Node.SCHEMA_RECALL, AsyncNodeAction.node_async(schemeReCallNode))
                .addNode(DataAgentSpec.Graph.Node.TABLE_RELATION, AsyncNodeAction.node_async(tableRelationNode))
                .addEdge(StateGraph.START, DataAgentSpec.Graph.Node.EVIDENCE_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, DataAgentSpec.Graph.Node.SCHEMA_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.SCHEMA_RECALL, DataAgentSpec.Graph.Node.TABLE_RELATION)
                .addEdge(DataAgentSpec.Graph.Node.TABLE_RELATION, StateGraph.END);
    }
}
