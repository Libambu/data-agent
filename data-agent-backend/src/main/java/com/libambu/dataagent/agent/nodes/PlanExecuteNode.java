package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Plan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 计划执行节点。
 * <p>
 * 根据当前步骤号从执行计划中取出对应步骤，决定下一个要执行的工具节点。
 * 当所有步骤执行完毕后，返回 END。
 */
@Slf4j
@Component
public class PlanExecuteNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Plan plan = Plan.getPlan(state);
        Integer currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        List<Plan.ExecutionStep> steps = plan.getExecutionPlan();

        if (currentStep > steps.size()) {
            log.info("[PlanExecuteNode] 计划执行完毕, 当前步骤: {}, 总步骤: {}", currentStep, steps.size());
            return Map.of(
                    DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1,
                    DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END
            );
        }

        String nextTool = steps.get(currentStep - 1).getToolToUse();
        log.info("[PlanExecuteNode] 执行步骤 {}/{}, 工具: {}", currentStep, steps.size(), nextTool);
        return Map.of(
                DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, nextTool
        );
    }
}
