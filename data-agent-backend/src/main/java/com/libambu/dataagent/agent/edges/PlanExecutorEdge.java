package com.libambu.dataagent.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * 计划执行后的条件边。
 * <p>
 * 根据 PlanExecuteNode 写入的 NEXT_NODE 决定下一步走向：
 * SQL_GENERATION / END
 */
@Slf4j
public class PlanExecutorEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextNode = state.value(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END);
        if (StateGraph.END.equals(nextNode)) {
            log.info("[PlanExecutorEdge] 计划执行完毕，进入 END");
            return StateGraph.END;
        }
        log.info("[PlanExecutorEdge] 下一节点: {}", nextNode);
        return nextNode;
    }
}
