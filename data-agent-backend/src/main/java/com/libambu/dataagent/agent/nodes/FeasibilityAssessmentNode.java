package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 可行性评估节点。
 * <p>
 * 结合规范化问题、召回 Schema、Evidence 和多轮上下文，判断是否进入数据分析规划流程。
 */
@Component
@Slf4j
public class FeasibilityAssessmentNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private PromptManager promptManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String tableRelation = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, "");
        if (tableRelation == null || tableRelation.isBlank()) {
            throw new IllegalStateException("Unable to read Schema");
        }
        Schema schema = objectMapper.readValue(tableRelation, Schema.class);
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        String multiTurn = state.value(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, "(无)");

        String prompt = promptManager.getFeasibilityAssessmentPromptTemplate().render(Map.of(
                "recalled_schema", schema.buildSchemePrompt(),
                "evidence", evidence,
                "canonical_query", rewriteQuery,
                "multi_turn", multiTurn
        ));

        String feasibilityAssessment = deepseekClient
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .user(prompt)
                .call()
                .content();
        if (feasibilityAssessment == null || feasibilityAssessment.isBlank()) {
            throw new IllegalArgumentException("feasible assessment fail");
        }
        log.info("feasibilityAssessment: {}", feasibilityAssessment);
        return Map.of(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, feasibilityAssessment);
    }
}
