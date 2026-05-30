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
 * Python 代码生成节点。
 * <p>
 * 根据当前执行计划步骤中的 instruction，结合 Schema 与 SQL 执行结果生成 Python 分析代码。
 */
@Slf4j
@Component
public class PythonGeneratorNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private PromptManager promptManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("[PythonGeneratorNode] 开始生成 Python 代码");
        Integer currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);

        try {
            String tableRelation = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, "");
            Schema schema = objectMapper.readValue(tableRelation, Schema.class);

            SqlExecuteNode.SqlExecuteResult result = objectMapper.convertValue(
                    state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, Object.class).orElseThrow(
                            () -> new RuntimeException("PYTHON_GENERATION 缺少前置 SQL 执行结果")
                    ),
                    SqlExecuteNode.SqlExecuteResult.class
            );

            Plan.ExecutionStep executionStep = Plan.getCurrentStep(state);

            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("python_memory", "500");
            vars.put("python_timeout", "500");
            vars.put("database_schema", schema.buildSchemePrompt());
            vars.put("sample_input", objectMapper.writeValueAsString(result.getResultSet().getData()));
            vars.put("plan_description", objectMapper.writeValueAsString(executionStep.getToolParameters()));

            String prompt = promptManager.getPythonGeneratorPromptTemplate().render(vars);

            log.info("[PythonGeneratorNode] Python 生成提示词: {}", prompt);

            String pythonCode = deepseekClient
                    .prompt()
                    .system(prompt)
                    .options(OpenAiChatOptions.builder()
                            .extraBody(Map.of("enable_thinking", false)))
                    .call()
                    .content();

            log.info("[PythonGeneratorNode] 生成 Python 代码: {}", pythonCode);
            return Map.of(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT,
                    MarkdownParserUtil.extractRawText(pythonCode));
        } catch (Exception ex) {
            log.error("[PythonGeneratorNode] Python 代码生成失败 step={}, err={}",
                    currentStep, ex.getMessage(), ex);
            Map<String, String> deltaOutput = new HashMap<>();
            // 阶段化错误 key：与 SQL 链路（_sqlgen_error/_sqlexec_error）对齐，
            // 避免后续 PythonExecuteNode/PythonAnalyzeNode 在同一个 step 上写 _error 时
            // 通过 MERGE 策略覆盖掉真正的根因（Python 代码生成失败）。
            deltaOutput.put("step_" + currentStep + "_pygen_error",
                    "PYTHON_GENERATION 失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            Map<String, Object> result = new HashMap<>();
            // 清空旧值（put null 触发框架删除），避免下游 PythonExecuteNode 误用残留代码
            result.put(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, null);
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            return result;
        }
    }
}
