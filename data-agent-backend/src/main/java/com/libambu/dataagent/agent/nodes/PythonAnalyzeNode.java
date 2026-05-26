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
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("[PythonAnalyzeNode] 开始分析 Python 执行结果");

        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");

        PythonExecutionResult pythonOutput = objectMapper.convertValue(
                state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, Object.class).orElseThrow(),
                PythonExecutionResult.class
        );

        Integer step = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);

        Map<String, String> executionOutput = (Map<String, String>) state.value(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, Object.class
        ).orElse(new HashMap<>());

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

        // 将分析结果写入执行输出
        Map<String, String> updatedOutput = new HashMap<>(executionOutput);
        updatedOutput.put("step_" + step + "_analysis", analyze);

        return Map.of(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, updatedOutput,
                DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, step + 1
        );
    }
}
