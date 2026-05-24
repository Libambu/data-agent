package com.libambu.dataagent.agent.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.extern.slf4j.Slf4j;

/**
 * 可行性评估后的条件边。
 * <p>
 * 只有被判定为数据分析类需求时才进入 Planner，否则直接结束并把评估结果返回给调用方。
 */
@Slf4j
public class FeasibilityAssessmentEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String output = state.value(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, "");
        if (output.contains("【需求类型】：《数据分析》")) {
            log.info("[FeasibilityAssessmentNodeEdge]需求类型为数据分析，进入PlannerNode节点");
            return DataAgentSpec.Graph.Node.PLANNER;
        }
        log.info("[FeasibilityAssessmentNodeEdge]需求类型非数据分析，返回END节点");
        return StateGraph.END;
    }
}
