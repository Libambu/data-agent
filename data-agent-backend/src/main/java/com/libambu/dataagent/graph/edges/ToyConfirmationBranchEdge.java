package com.libambu.dataagent.graph.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import org.springframework.stereotype.Component;

/**
    如果用户没有确认通过：直接返回 END
    如果用户确认通过：读取之前 ROUTE 保存的 scene
    如果 scene = travel，进入 TRAVEL_PLAN_NODE
    如果 scene = study，进入 STUDY_PLAN_NODE
 */
@Component
public class ToyConfirmationBranchEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        // 如果确认不通过，则返回结束
        Boolean approved = state.value(ToyGraphSpec.StateKey.CONFIRMATION_APPROVED, false);
        if (!approved) {
            return StateGraph.END;
        }
        return state.value(ToyGraphSpec.StateKey.SCENE, ToyGraphSpec.Scene.STUDY);
    }
}
