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

        // 上游 SQL 生成失败时（结果为空），直接归档错误，避免空 SQL 进入数据库执行。
        if (sql == null || sql.isBlank()) {
            log.warn("[SqlExecuteNode] 上游 SQL 为空，跳过执行 step={}", currentStep);
            Map<String, String> deltaOutput = new HashMap<>();
            deltaOutput.put("step_" + currentStep + "_error",
                    "SQL_EXECUTION 跳过：上游未生成有效 SQL");
            Map<String, Object> result = new HashMap<>();
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            return result;
        }

        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");

        try {
            SqlResultSet resultSetWrapper;
            try (Connection connection = schemaDataSourceProvider.get(databaseId).getConnection();
                 Statement stmt = connection.createStatement()) {
                resultSetWrapper = ResultSetBuilder.buildFrom(stmt.executeQuery(sql));
            }

            log.info("[SqlExecuteNode] SQL 执行结果: {}", resultSetWrapper);

            DisplaySpec displaySpec = buildDisplaySpec(resultSetWrapper);
            log.info("[SqlExecuteNode] 展示规格: {}", displaySpec);

            // 把生成的 SQL 回填到当前步骤，并将 Plan 序列化回 state，
            // 否则下游 Supervisor / ReportGenerator 反序列化时拿不到 sqlQuery 字段。
            Plan plan = Plan.getPlan(state);
            Plan.ExecutionStep currentStepDetail = plan.getExecutionPlan().get(currentStep - 1);
            currentStepDetail.getToolParameters().setSqlQuery(sql);
            String updatedPlanJson = objectMapper.writeValueAsString(plan);

            Map<String, Object> result = new HashMap<>();
            // 注意：CURRENT_STEP 不再由 Sub-Agent 推进，统一交给 SupervisorNode 决策时计算。
            result.put(DataAgentSpec.Graph.StateKey.Planning.PLAN, updatedPlanJson);
            result.put(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT,
                    new SqlExecuteResult(resultSetWrapper, displaySpec));
            // EXECUTION_OUTPUT 在 GraphConfiguration 中已配置 MERGE 策略，这里只 put 自己这一步即可。
            Map<String, String> executionOutput = new HashMap<>();
            executionOutput.put("step_" + currentStep, objectMapper.writeValueAsString(resultSetWrapper));
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, executionOutput);
            return result;
        } catch (Exception ex) {
            // 兜底：把错误归档给 Supervisor 决定重试还是换路径，不向上抛出。
            // 常见错误：SQL 语法错误、表/列不存在、连接超时等。
            log.error("[SqlExecuteNode] SQL 执行失败 step={}, sql={}, err={}",
                    currentStep, sql, ex.getMessage(), ex);
            Map<String, String> deltaOutput = new HashMap<>();
            String errSummary = "SQL_EXECUTION 失败: " + ex.getClass().getSimpleName()
                    + " - " + ex.getMessage()
                    + " | 失败的 SQL: " + sql;
            deltaOutput.put("step_" + currentStep + "_error", errSummary);
            Map<String, Object> result = new HashMap<>();
            result.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, deltaOutput);
            // 注意：故意不清空 SQL_EXECUTION_RESULT，保留之前成功的数据，
            // 让多 SQL 场景下早期的成功结果仍可被后续步骤/报告使用。
            // Supervisor 会通过 trace 中的 ❌ 失败标记自行判断当前这步是否成功。
            return result;
        }
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
