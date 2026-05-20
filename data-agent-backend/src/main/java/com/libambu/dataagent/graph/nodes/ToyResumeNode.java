package com.libambu.dataagent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class ToyResumeNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        return Collections.emptyMap();
    }
}
