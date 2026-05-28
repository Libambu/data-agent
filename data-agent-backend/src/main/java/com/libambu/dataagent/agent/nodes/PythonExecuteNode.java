package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.PythonExecutionResult;
import com.libambu.dataagent.utils.SimplePythonExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Python 代码执行节点。
 * <p>
 * 执行 PYTHON_GENERATION 节点生成的 Python 代码，将 SQL 执行结果作为输入数据传入。
 */
@Slf4j
@Component
public class PythonExecuteNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Integer currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        String pythonCode = state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, "");

        // 上游 Python 代码生成失败 → 跳过执行，归档错误
        if (pythonCode == null || pythonCode.isBlank()) {
            log.warn("[PythonExecuteNode] 上游 Python 代码为空，跳过执行 step={}", currentStep);
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + currentStep + "_error",
                    "PYTHON_EXECUTION 跳过：上游未生成有效 Python 代码");
            Map<String, Object> result = new HashMap<>();
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            return result;
        }

        try {
            SqlExecuteNode.SqlExecuteResult result = objectMapper.convertValue(
                    state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, Object.class).orElseThrow(
                            () -> new RuntimeException("PYTHON_EXECUTION 缺少前置 SQL 执行结果")
                    ),
                    SqlExecuteNode.SqlExecuteResult.class
            );

            String inputDataJson = objectMapper.writeValueAsString(result.getResultSet().getData());
            if (inputDataJson == null || inputDataJson.isBlank()) {
                throw new RuntimeException("sql结果为空");
            }

            log.info("[PythonExecuteNode] 开始执行 Python 代码");
            PythonExecutionResult output = SimplePythonExecutor.execute(pythonCode, inputDataJson);
            log.info("[PythonExecuteNode] Python 执行结果: success={}, output={}",
                    output.isSuccess(), output.getOutput());

            // SimplePythonExecutor 不抛异常，而是把失败放进 success=false 的结果里。
            // 这里把 success=false 也作为"执行错误"上报给 Supervisor，避免下游 Analyze 误以为成功。
            if (!output.isSuccess()) {
                Map<String, String> deltaOutput = new HashMap<>();
                deltaOutput.put("step_" + currentStep + "_error",
                        "PYTHON_EXECUTION 运行失败: " + truncate(String.valueOf(output.getOutput()), 600));
                Map<String, Object> ret = new HashMap<>();
                ret.put(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, output);
                ret.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
                return ret;
            }

            return Map.of(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, output);
        } catch (Exception ex) {
            log.error("[PythonExecuteNode] Python 执行节点抛出异常 step={}, err={}",
                    currentStep, ex.getMessage(), ex);
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + currentStep + "_error",
                    "PYTHON_EXECUTION 异常: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            Map<String, Object> ret = new HashMap<>();
            ret.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            // 故意不清空 PYTHON_EXECUTION_RESULT，保留之前可能存在的成功结果，
            // 由 Supervisor 通过 trace 中的 ❌ 失败标记判断当前这步是否成功。
            return ret;
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...(truncated)";
    }
}
