package com.libambu.dataagent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ToySceneRouterNode implements NodeAction {

    private static final List<String> TRAVEL_KEYWORDS = List.of(
            "旅游", "旅行", "出游", "攻略", "景点", "酒店", "美食", "周末去哪"
    );

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String input = state.value(ToyGraphSpec.StateKey.INPUT, "");
        String normalizedInput = input.toLowerCase(Locale.ROOT);

        boolean travel = TRAVEL_KEYWORDS.stream()
                .anyMatch(keyword -> normalizedInput.contains(keyword.toLowerCase(Locale.ROOT)));
        String scene = travel ? ToyGraphSpec.Scene.TRAVEL : ToyGraphSpec.Scene.STUDY;
        String sceneLabel = travel ? "旅行攻略" : "学习计划";

        return Map.of(
                ToyGraphSpec.StateKey.SCENE, scene,
                ToyGraphSpec.StateKey.SCENE_LABEL, sceneLabel
        );
    }
}
