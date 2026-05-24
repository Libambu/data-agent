package com.libambu.dataagent.entity.dto;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Planner 节点输出的执行计划模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("thought_process")
    @JsonPropertyDescription("简要描述你的分析思路。必须明确提到你检查了哪些表和字段")
    private String thoughtProcess;

    @JsonProperty("execution_plan")
    @JsonPropertyDescription("执行计划的步骤列表")
    private List<ExecutionStep> executionPlan = new ArrayList<>();

    public static Plan getPlan(OverAllState state) {
        String planStr = state.value(DataAgentSpec.Graph.StateKey.Planning.PLAN, "");
        try {
            return OBJECT_MAPPER.readValue(planStr, Plan.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("plan json反序列化失败", e);
        }
    }

    public static ExecutionStep getCurrentStep(OverAllState state) {
        Plan plan = getPlan(state);
        Integer step = state.value(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, 1);
        return plan.getExecutionPlan().get(step - 1);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep {

        @JsonProperty("step")
        @JsonPropertyDescription("步骤顺序号")
        private int step;

        @JsonProperty("tool_to_use")
        @JsonPropertyDescription("工具名称")
        private String toolToUse;

        @JsonProperty("tool_parameters")
        @JsonPropertyDescription("工具参数")
        private ToolParameters toolParameters;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolParameters {

        @JsonProperty("instruction")
        @JsonPropertyDescription("当工具名称tool_to_use是DataAgentSpec.Graph.Node.SQL_GENERATION时这里的值为当前步骤要做的详细 SQL 需求，是DataAgentSpec.Graph.Node.PYTHON_GENERATION时填当前步骤要做的详细编程需求")
        private String instruction;

        @JsonProperty("summary_and_recommendations")
        @JsonPropertyDescription("DataAgentSpec.Graph.Node.REPORT_GENERATION节点专用，报告的大纲")
        private String summaryAndRecommendations;

        @JsonProperty("sql_query")
        @JsonPropertyDescription("DataAgentSpec.Graph.Node.SQL_GENERATION 运行完成后，会把生成的 SQL 填入")
        private String sqlQuery;
    }
}
