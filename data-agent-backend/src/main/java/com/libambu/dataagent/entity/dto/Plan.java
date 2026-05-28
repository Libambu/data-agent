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

    /**
     * Supervisor 模式下，把决策出的新 step 序列化回 state 中的 PLAN 字段。
     * <p>
     * 用于 SupervisorNode 每轮决策后，把"下一步要派给哪个 Sub-Agent + 任务参数"作为新的 ExecutionStep
     * 追加到 Plan.executionPlan 末尾，使下游 Sub-Agent 可以继续通过 {@link #getCurrentStep(OverAllState)} 读取。
     *
     * @param state    当前图状态
     * @param toolName 下一步要执行的工具节点名（SQL_GENERATION/PYTHON_GENERATION/REPORT_GENERATION）
     * @param params   该步骤的工具参数
     * @return 序列化后的最新 plan json
     */
    public static String appendStep(OverAllState state, String toolName, ToolParameters params) {
        Plan plan;
        try {
            plan = getPlan(state);
        } catch (IllegalArgumentException e) {
            // 第一次还没有计划，构造一个空的
            plan = new Plan();
            plan.setThoughtProcess("Supervisor dynamic plan");
            plan.setExecutionPlan(new ArrayList<>());
        }
        if (plan.getExecutionPlan() == null) {
            plan.setExecutionPlan(new ArrayList<>());
        }
        ExecutionStep step = new ExecutionStep();
        step.setStep(plan.getExecutionPlan().size() + 1);
        step.setToolToUse(toolName);
        step.setToolParameters(params == null ? new ToolParameters() : params);
        plan.getExecutionPlan().add(step);
        try {
            return OBJECT_MAPPER.writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("plan json 序列化失败", e);
        }
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
