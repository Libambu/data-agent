package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.datasource.SchemaDataSourceProvider;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.DisplaySpec;
import com.libambu.dataagent.entity.dto.Plan;
import com.libambu.dataagent.entity.dto.SqlResultSet;
import com.libambu.dataagent.utils.ResultSetBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 执行节点。
 * <p>
 * 执行 SQL_GENERATION 节点生成的 SQL，将结果集写入状态，并推进计划步骤。
 */
@Slf4j
@Component
public class SqlExecuteNode implements NodeAction {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private SchemaDataSourceProvider schemaDataSourceProvider;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Integer currentStep = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        String sql = state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");

        SqlResultSet resultSetWrapper;
        try (Connection connection = schemaDataSourceProvider.get(databaseId).getConnection();
             Statement stmt = connection.createStatement()) {
            resultSetWrapper = ResultSetBuilder.buildFrom(stmt.executeQuery(sql));
        }

        log.info("[SqlExecuteNode] SQL 执行结果: {}", resultSetWrapper);

        DisplaySpec displaySpec = buildDisplaySpec(resultSetWrapper);
        log.info("[SqlExecuteNode] 展示规格: {}", displaySpec);

        // 将生成的 SQL 回填到当前步骤
        Plan.getCurrentStep(state).getToolParameters().setSqlQuery(sql);

        Map<String, Object> result = new HashMap<>();
        result.put(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, currentStep + 1);
        result.put(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT,
                new SqlExecuteResult(resultSetWrapper, displaySpec));
        result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT,
                Map.of("step_" + currentStep, objectMapper.writeValueAsString(resultSetWrapper)));
        return result;
    }

    private DisplaySpec buildDisplaySpec(SqlResultSet resultSetWrapper) {
        if (resultSetWrapper.getColumn().isEmpty()) {
            return new DisplaySpec("table", "SQL已生成，等待外部执行", null, Collections.emptyList());
        }
        String xAxis = resultSetWrapper.getColumn().get(0);
        List<String> yAxis = resultSetWrapper.getColumn().subList(1, resultSetWrapper.getColumn().size());
        return new DisplaySpec("table", "SQL执行结果", xAxis, yAxis);
    }

    /**
     * SQL 执行结果封装。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SqlExecuteResult {
        private SqlResultSet resultSet;
        private DisplaySpec display;
    }
}
