package com.libambu.dataagent.config;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import com.libambu.dataagent.graph.edges.ToySceneBranchEdge;
import com.libambu.dataagent.graph.nodes.ToySceneRouterNode;
import com.libambu.dataagent.graph.nodes.ToyStudyPlanNode;
import com.libambu.dataagent.graph.nodes.ToyTravelPlanNode;
import com.libambu.dataagent.graph.nodes.ToyWrapUpNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Graph 配置，对齐 Kotlin 版本的多节点分支工作流：
 * START -> ROUTE_NODE -> TRAVEL_PLAN_NODE / STUDY_PLAN_NODE -> WRAP_UP_NODE -> END
 */
@Configuration
public class GraphConfiguration {

    @Bean
    public StateGraph toyBranchStreamingGraph(ToySceneRouterNode toySceneRouterNode,
                                              ToySceneBranchEdge toySceneBranchEdge,
                                              ToyTravelPlanNode toyTravelPlanNode,
                                              ToyStudyPlanNode toyStudyPlanNode,
                                              ToyWrapUpNode toyWrapUpNode) throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> Map.of(
                ToyGraphSpec.StateKey.INPUT, KeyStrategy.REPLACE,
                ToyGraphSpec.StateKey.SCENE, KeyStrategy.REPLACE,
                ToyGraphSpec.StateKey.SCENE_LABEL, KeyStrategy.REPLACE,
                ToyGraphSpec.StateKey.DRAFT, KeyStrategy.REPLACE,
                ToyGraphSpec.StateKey.FINAL_OUTPUT, KeyStrategy.REPLACE
        );

        return new StateGraph(ToyGraphSpec.NAME, keyStrategyFactory)
                .addNode(ToyGraphSpec.Node.ROUTE, AsyncNodeAction.node_async(toySceneRouterNode))
                .addNode(ToyGraphSpec.Node.TRAVEL_PLAN, AsyncNodeAction.node_async(toyTravelPlanNode))
                .addNode(ToyGraphSpec.Node.STUDY_PLAN, AsyncNodeAction.node_async(toyStudyPlanNode))
                .addNode(ToyGraphSpec.Node.WRAP_UP, AsyncNodeAction.node_async(toyWrapUpNode))
                .addEdge(StateGraph.START, ToyGraphSpec.Node.ROUTE)
                .addConditionalEdges(
                        ToyGraphSpec.Node.ROUTE,
                        AsyncEdgeAction.edge_async(toySceneBranchEdge),
                        //如果条件边返回 travel，下一步进入 TRAVEL_PLAN_NODE,如果条件边返回 study，下一步进入 STUDY_PLAN_NODE
                        Map.of(
                                ToyGraphSpec.Scene.TRAVEL, ToyGraphSpec.Node.TRAVEL_PLAN,
                                ToyGraphSpec.Scene.STUDY, ToyGraphSpec.Node.STUDY_PLAN
                        )
                )
                .addEdge(ToyGraphSpec.Node.TRAVEL_PLAN, ToyGraphSpec.Node.WRAP_UP)
                .addEdge(ToyGraphSpec.Node.STUDY_PLAN, ToyGraphSpec.Node.WRAP_UP)
                .addEdge(ToyGraphSpec.Node.WRAP_UP, StateGraph.END);
    }
}
