package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Plan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告生成节点。
 * <p>
 * 将执行计划的所有步骤结果汇总，生成面向用户的自然语言 Markdown 报告（流式输出）。
 */
@Slf4j
@Component
public class ReportGeneratorNode implements NodeAction {

    private static final String CLEAN_JSON_EXAMPLE = """
            {
                "title": { "text": "月度销售额" },
                "tooltip": { "trigger": "axis" },
                "xAxis": { "type": "category", "data": ["1月", "2月"] },
                "yAxis": { "type": "value" },
                "series": [
                    { "type": "bar", "data": [120, 200] }
                ]
            }""";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private PromptManager promptManager;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("[ReportGeneratorNode] 开始生成报告");

        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        Plan plan = Plan.getPlan(state);

        Map<String, String> executionResults = (Map<String, String>) state.value(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, Object.class
        ).orElse(Collections.emptyMap());

        String summaryAndRecommendations = Plan.getCurrentStep(state).getToolParameters().getSummaryAndRecommendations();
        if (summaryAndRecommendations == null) {
            summaryAndRecommendations = "";
        }

        Flux<String> reportFlux = generateReport(rewriteQuery, plan, executionResults, summaryAndRecommendations);
        return Map.of(DataAgentSpec.Graph.StateKey.Execution.REPORT_RESULT, reportFlux);
    }

    private Flux<String> generateReport(String userInput, Plan plan,
                                        Map<String, String> executionResults,
                                        String summaryAndRecommendations) {
        String userRequirementsAndPlan = buildUserRequirementsAndPlan(userInput, plan);
        String analysisStepsAndData = buildAnalysisStepsAndData(plan, executionResults);

        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("user_requirements_and_plan", userRequirementsAndPlan);
        vars.put("analysis_steps_and_data", analysisStepsAndData);
        vars.put("summary_and_recommendations", summaryAndRecommendations);
        vars.put("json_example", CLEAN_JSON_EXAMPLE);
        vars.put("optimization_section", "");

        String reportPrompt = promptManager.getReportGeneratorPlainPromptTemplate().render(vars);
        log.info("[ReportGeneratorNode] Report Node Prompt: \n {} \n", reportPrompt);

        return deepseekClient
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .user(reportPrompt)
                .stream()
                .content();
    }

    /**
     * 构建用户需求和计划描述。
     */
    private String buildUserRequirementsAndPlan(String userInput, Plan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户原始需求\n");
        sb.append(userInput).append("\n\n");

        sb.append("## 执行计划概述\n");
        sb.append("**思考过程**: ").append(plan.getThoughtProcess()).append("\n\n");

        sb.append("## 详细执行步骤\n");
        List<Plan.ExecutionStep> steps = plan.getExecutionPlan();
        for (int i = 0; i < steps.size(); i++) {
            Plan.ExecutionStep step = steps.get(i);
            sb.append("### 步骤 ").append(i + 1).append(": 步骤编号 ").append(step.getStep()).append("\n");
            sb.append("**工具**: ").append(step.getToolToUse()).append("\n");
            if (step.getToolParameters() != null) {
                sb.append("**参数描述**: ").append(step.getToolParameters().getInstruction()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建分析步骤和数据结果描述。
     */
    private String buildAnalysisStepsAndData(Plan plan, Map<String, String> executionResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 数据执行结果\n");

        if (executionResults == null || executionResults.isEmpty()) {
            sb.append("暂无执行结果数据\n");
            return sb.toString();
        }

        List<Plan.ExecutionStep> executionPlan = plan.getExecutionPlan() != null
                ? plan.getExecutionPlan()
                : Collections.emptyList();

        for (Map.Entry<String, String> entry : executionResults.entrySet()) {
            String stepKey = entry.getKey();
            String stepResult = entry.getValue();

            // 过滤掉以 _analysis 结尾的 key
            if (stepKey.endsWith("_analysis")) {
                continue;
            }

            sb.append("### ").append(stepKey).append("\n");

            // 尝试获取对应的步骤描述
            try {
                int stepIndex = Integer.parseInt(stepKey.replace("step_", "")) - 1;
                if (stepIndex >= 0 && stepIndex < executionPlan.size()) {
                    Plan.ExecutionStep step = executionPlan.get(stepIndex);
                    sb.append("**步骤编号**: ").append(step.getStep()).append("\n");
                    sb.append("**使用工具**: ").append(step.getToolToUse()).append("\n");

                    if (step.getToolParameters() != null) {
                        sb.append("**参数描述**: ").append(step.getToolParameters().getInstruction()).append("\n");
                        if (step.getToolParameters().getSqlQuery() != null) {
                            sb.append("**执行SQL**: \n```sql\n").append(step.getToolParameters().getSqlQuery()).append("\n```\n");
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }

            sb.append("**执行结果**: \n```json\n").append(stepResult).append("\n```\n\n");

            // 检查是否有对应的分析结果
            String analysisKey = stepKey + "_analysis";
            String analysisResult = executionResults.get(analysisKey);
            if (analysisResult != null && !analysisResult.isBlank()) {
                sb.append("**Python 分析结果**: ").append(analysisResult).append(" ");
            }
        }
        return sb.toString();
    }
}
