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

import java.util.HashMap;
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
        // Supervisor 派单时已经写好 CURRENT_STEP，这里只读，用作错误归档时的 step 标识。
        Integer currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        try {
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
            return Map.of(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT,
                    MarkdownParserUtil.extractRawText(sql));
        } catch (Exception ex) {
            // 兜底：不再向上抛出，避免整张图崩溃；
            // 把错误写入 EXECUTION_OUTPUT，下一轮 Supervisor 会读到并决定重试或换路径。
            // 注意：同时写入 step_N_error 与 step_N_sqlgen_error 两个 key——
            //   - step_N_error 用于通用失败标记（兼容旧路径与 Supervisor 的失败判定）；
            //   - step_N_sqlgen_error 单独保留 SQL 生成阶段的错误信息，避免后续
            //     SqlExecuteNode 因 SQL 为空再次写 step_N_error 时把"真正的根因"覆盖掉。
            log.error("[SqlGeneratorNode] SQL 生成失败 step={}, err={}", currentStep, ex.getMessage(), ex);
            String errSummary = "SQL_GENERATION 失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage();
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + currentStep + "_error", errSummary);
            deltaOutput.put("step_" + currentStep + "_sqlgen_error", errSummary);
            Map<String, Object> result = new HashMap<>();
            // 清空旧值（put null 触发框架删除），避免下游 SqlExecuteNode 误用上一轮 SQL 文本
            result.put(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, null);
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            return result;
        }
    }
}
