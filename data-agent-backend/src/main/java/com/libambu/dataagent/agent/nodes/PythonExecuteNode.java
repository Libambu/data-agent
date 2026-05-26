package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.PythonExecutionResult;
import com.libambu.dataagent.utils.SimplePythonExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
        String pythonCode = state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, "");

        SqlExecuteNode.SqlExecuteResult result = objectMapper.convertValue(
                state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, Object.class).orElseThrow(),
                SqlExecuteNode.SqlExecuteResult.class
        );

        String inputDataJson = objectMapper.writeValueAsString(result.getResultSet().getData());
        if (inputDataJson == null || inputDataJson.isBlank()) {
            throw new RuntimeException("sql结果为空");
        }

        log.info("[PythonExecuteNode] 开始执行 Python 代码");
        PythonExecutionResult output = SimplePythonExecutor.execute(pythonCode, inputDataJson);
        log.info("[PythonExecuteNode] Python 执行结果: success={}, output={}", output.isSuccess(), output.getOutput());

        return Map.of(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, output);
    }
}
