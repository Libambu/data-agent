package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.PythonExecutionResult;
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
 * Python 结果分析节点。
 * <p>
 * 对 Python 执行结果进行自然语言总结，并推进计划步骤。
 */
@Slf4j
@Component
public class PythonAnalyzeNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private PromptManager promptManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("[PythonAnalyzeNode] 开始分析 Python 执行结果");
        Integer step = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);

        try {
            String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");

            // 上游可能因为 Python 生成/执行失败而没有 PYTHON_EXECUTION_RESULT，
            // 直接跳过分析，把失败信号透传给 Supervisor。
            var pythonOutputOpt = state.value(
                    DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, Object.class);
            if (pythonOutputOpt.isEmpty()) {
                log.warn("[PythonAnalyzeNode] 上游 Python 执行结果缺失，跳过分析 step={}", step);
                Map<String, String> deltaOutput = new HashMap<>();
                // 阶段化 skipped key，不覆盖上游 _pygen_error / _pyexec_error
                deltaOutput.put("step_" + step + "_pyanalyze_skipped",
                        "PYTHON_ANALYSIS 跳过：上游 Python 执行结果缺失");
                return Map.of(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            }

            PythonExecutionResult pythonOutput = objectMapper.convertValue(
                    pythonOutputOpt.get(), PythonExecutionResult.class);

            // Python 执行未成功 → 不要让 LLM 凭空"分析"错误信息，直接归档为错误供 Supervisor 决策
            if (!pythonOutput.isSuccess()) {
                log.warn("[PythonAnalyzeNode] Python 执行未成功，跳过分析 step={}", step);
                Map<String, String> deltaOutput = new HashMap<>();
                // 阶段化 skipped key，不覆盖上游 _pyexec_error
                deltaOutput.put("step_" + step + "_pyanalyze_skipped",
                        "PYTHON_ANALYSIS 跳过：Python 执行未成功");
                return Map.of(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            }

            Map<String, Object> vars = new LinkedHashMap<>();
            vars.put("python_output", pythonOutput);
            vars.put("user_query", rewriteQuery);

            String prompt = promptManager.getPythonAnalyzePromptTemplate().render(vars);

            String analyze = deepseekClient
                    .prompt()
                    .options(OpenAiChatOptions.builder()
                            .extraBody(Map.of("enable_thinking", false)))
                    .user(prompt)
                    .call()
                    .content();

            if (analyze == null) {
                analyze = "";
            }

            log.info("[PythonAnalyzeNode] 分析结果: {}", analyze);

            // EXECUTION_OUTPUT 在 GraphConfiguration 中已配置 MERGE 策略，这里只 put 自己产出的 entry，
            // 框架会自动按 Map.putAll 合并到全局累积结果中，不会覆盖前序步骤的数据。
            // CURRENT_STEP 也不再由 Sub-Agent 推进，统一交给 SupervisorNode 在下一轮决策时计算。
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + step + "_analysis", analyze);

            return Map.of(
                    DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput
            );
        } catch (Exception ex) {
            log.error("[PythonAnalyzeNode] 分析失败 step={}, err={}", step, ex.getMessage(), ex);
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + step + "_pyanalyze_error",
                    "PYTHON_ANALYSIS 失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            return Map.of(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
        }
    }
}
