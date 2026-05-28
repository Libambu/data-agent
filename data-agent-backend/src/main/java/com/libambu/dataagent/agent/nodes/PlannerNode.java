package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Plan;
import com.libambu.dataagent.entity.dto.Schema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务拆解节点（草稿计划生成器）。
 * <p>
 * 在 Supervisor 模式下，本节点的职责由"权威执行计划生成者"降级为"草稿计划生成者"：
 * 1. 生成一份初步的整体计划，供 HumanFeedbackNode 展示给用户审核。
 * 2. 用户批准后，真正的逐步派单交给 {@link SupervisorNode}（主管 Agent）动态决策。
 *    Supervisor 会基于"用户问题 + 历次执行结果"在每一轮重新选择 Sub-Agent，
 *    不再严格按本节点输出的 step 顺序执行。
 * <p>
 * 因此本节点输出的 Plan 主要用于：人工审核展示、最终报告生成时的"思考过程"上下文。
 */
@Component
public class PlannerNode implements NodeAction {

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
        String feedbackContent = state.value(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        BeanOutputConverter<Plan> beanOutputConverter = new BeanOutputConverter<>(Plan.class);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("user_question", rewriteQuery);
        vars.put("schema", schema.buildSchemePrompt());
        vars.put("evidence", evidence);
        vars.put("semantic_model", "");
        vars.put("plan_validation_error", feedbackContent);
        vars.put("format", beanOutputConverter.getFormat());

        String prompt = promptManager.getPlannerPromptTemplate().render(vars);
        String plan = deepseekClient
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .user(prompt)
                .call()
                .content();
        if (plan == null || plan.isBlank()) {
            throw new IllegalArgumentException("plan generate fail");
        }
        return Map.of(DataAgentSpec.Graph.StateKey.Planning.PLAN, plan);
    }
}
