package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Plan;
import com.libambu.dataagent.entity.dto.Schema;
import com.libambu.dataagent.utils.MarkdownParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL 生成节点。
 * <p>
 * 根据当前执行计划步骤中的 instruction，结合 Schema 与 Evidence 生成 SQL。
 */
@Slf4j
@Component
public class SqlGeneratorNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private PromptManager promptManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Plan.ExecutionStep step = Plan.getCurrentStep(state);
        String instruction = step.getToolParameters().getInstruction();
        if (instruction == null || instruction.isBlank()) {
            throw new RuntimeException("SQL 生成步骤 instruction 为空");
        }

        String tableRelation = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, "");
        Schema schema = objectMapper.readValue(tableRelation, Schema.class);
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        String dialect = "mysql";

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("dialect", dialect);
        vars.put("question", rewriteQuery);
        vars.put("schema_info", schema.buildSchemePrompt());
        vars.put("evidence", evidence);
        vars.put("execution_description", instruction);

        String sqlPrompt = promptManager.getNewSqlGeneratorPromptTemplate().render(vars);

        log.info("[SqlGeneratorNode] SQL 生成提示词: {}", sqlPrompt);

        String sql = deepseekClient
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .system(sqlPrompt)
                .call()
                .content();

        log.info("[SqlGeneratorNode] 生成 SQL: {}", sql);
        return Map.of(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, MarkdownParserUtil.extractRawText(sql));
    }
}
