package com.libambu.dataagent.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * Supervisor 决策后的条件边。
 * <p>
 * 根据 SupervisorNode 写入的 NEXT_NODE 决定下一步派给哪个 Sub-Agent：
 * SQL_GENERATION / PYTHON_GENERATION / REPORT_GENERATION / END
 */
@Slf4j
public class SupervisorEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String nextNode = state.value(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END);
        if (StateGraph.END.equals(nextNode)) {
            log.info("[SupervisorEdge] Supervisor 已决定结束，进入 END");
            return StateGraph.END;
        }
        log.info("[SupervisorEdge] Supervisor 派单 -> {}", nextNode);
        return nextNode;
    }
}
