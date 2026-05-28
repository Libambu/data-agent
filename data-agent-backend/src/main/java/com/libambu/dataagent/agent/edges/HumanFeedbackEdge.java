package com.libambu.dataagent.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * 人工审核后的条件边。
 * <p>
 * 根据 HumanFeedbackNode 写入的 NEXT_NODE 决定下一步走向：
 * SUPERVISOR / PLANNER / END
 */
@Slf4j
public class HumanFeedbackEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextNode = state.value(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, StateGraph.END);
        log.info("[HumanFeedbackEdge] 下一节点: {}", nextNode);
        return nextNode;
    }
}
