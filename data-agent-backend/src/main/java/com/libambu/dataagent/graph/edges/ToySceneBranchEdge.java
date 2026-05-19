package com.libambu.dataagent.graph.edges;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import org.springframework.stereotype.Component;

/**
 * 从全局状态 state 中读取 scene 如果 scene 不存在，默认返回 study
 */
@Component
public class ToySceneBranchEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        return state.value(ToyGraphSpec.StateKey.SCENE, ToyGraphSpec.Scene.STUDY);
    }
}
