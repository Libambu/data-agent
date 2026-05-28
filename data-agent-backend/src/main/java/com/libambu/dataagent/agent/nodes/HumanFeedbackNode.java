package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 人工审核节点。
 * <p>
 * 在 Planner 生成执行计划后，暂停等待用户确认。
 * 用户批准则进入 PlanExecution，拒绝则带反馈重新进入 Planner，
 * 修复次数超过 3 次则直接结束。
 */
@Slf4j
@Component
public class HumanFeedbackNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        Integer count = state.value(DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, 1);
        if (count >= 3) {
            log.info("[HumanFeedbackNode] 修复次数已达上限({}), 直接结束", count);
            return Map.of(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, StateGraph.END);
        }

        Boolean approved = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED, false);
        if (approved) {
            log.info("[HumanFeedbackNode] 用户已批准计划, 进入 Supervisor 节点");
            return Map.of(
                    DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, DataAgentSpec.Graph.Node.SUPERVISOR
            );
        } else {
            String feedbackContent = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, "");
            log.info("[HumanFeedbackNode] 用户拒绝计划, 反馈: {}, 重新进入Planner", feedbackContent);
            Map<String, Object> result = new HashMap<>();
            result.put(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, DataAgentSpec.Graph.Node.PLANNER);
            result.put(DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, count + 1);
            result.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK,
                    feedbackContent.isEmpty() ? "Plan rejected by user" : feedbackContent);
            return result;
        }
    }
}
