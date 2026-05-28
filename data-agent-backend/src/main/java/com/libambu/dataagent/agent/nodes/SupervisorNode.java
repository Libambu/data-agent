package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dto.Plan;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Supervisor 节点（主管 Agent）。
 * <p>
 * 不再按 Plan 数组下标静态取下一步，而是每一轮都让 LLM 基于"用户问题 + 已执行历史 + 上一步结果"
 * 动态决策：下一个该派给哪个 Sub-Agent（SQL_GENERATION / PYTHON_GENERATION / REPORT_GENERATION / END），
 * 以及给它什么任务参数。
 * <p>
 * 决策出的下一步会被作为新的 ExecutionStep 追加到 Plan 中，Sub-Agent 仍通过
 * {@link Plan#getCurrentStep(OverAllState)} 读取参数，因此对下游节点零侵入。
 * <p>
 * 同时保留 max_iterations 熔断，避免动态决策死循环。
 * <p>
 * 图节点 ID 为 {@code SUPERVISOR_NODE}，对应 {@link DataAgentSpec.Graph.Node#SUPERVISOR}。
 * 注：执行产出 state key 仍为 {@code PLAN_EXECUTE_NODE_OUTPUT}（保持兼容，避免破坏存量 trace 数据）。
 */
@Slf4j
@Component
public class SupervisorNode implements NodeAction {

    /** Supervisor 最大决策轮数，防止 LLM 死循环。 */
    private static final int MAX_ITERATIONS = 12;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // ===== 1. 读取 Supervisor 决策所需上下文 =====
        String userQuestion = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        // SUPERVISOR_ITERATION：Supervisor 已经派单的轮次（首次进入为 0）。
        // 不再依赖 CURRENT_STEP 来判断是否首轮，避免 SubAgent 也写 CURRENT_STEP 时的语义混乱。
        Integer iteration = state.value(DataAgentSpec.Graph.StateKey.Planning.SUPERVISOR_ITERATION, 0);

        Plan currentPlan;
        try {
            currentPlan = Plan.getPlan(state);
        } catch (Exception e) {
            currentPlan = new Plan();
        }
        if (currentPlan.getExecutionPlan() == null) {
            currentPlan.setExecutionPlan(new java.util.ArrayList<>());
        }

        // 首次进入 Supervisor（iteration==0）时，清空 PlannerNode 留下的草稿步骤，
        // 仅保留 thoughtProcess 留作最终报告使用，确保 Supervisor 从干净的状态开始动态派单。
        if (iteration == 0 && !currentPlan.getExecutionPlan().isEmpty()) {
            log.info("[Supervisor] 首次进入，清空草稿计划的 {} 个步骤，进入动态派单模式",
                    currentPlan.getExecutionPlan().size());
            currentPlan.getExecutionPlan().clear();
        }

        List<Plan.ExecutionStep> history = currentPlan.getExecutionPlan();

        // ===== 2. 熔断保护：超过最大迭代直接 END =====
        if (iteration >= MAX_ITERATIONS) {
            log.warn("[Supervisor] 已达最大迭代次数 {}, 强制结束", MAX_ITERATIONS);
            return Map.of(
                    DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END,
                    DataAgentSpec.Graph.StateKey.Planning.SUPERVISOR_ITERATION, iteration + 1
            );
        }

        // ===== 3. 收集已经发生的执行轨迹（供 Supervisor 观察） =====
        String agentTrace = buildAgentTrace(state, history);

        // ===== 4. 调用 LLM 做下一步决策 =====
        SupervisorDecision decision = decideNextStep(userQuestion, agentTrace);
        log.info("[Supervisor] 第 {} 轮决策结果: nextAgent={}, finished={}, thought={}",
                iteration + 1, decision.getNextAgent(), decision.isFinished(), decision.getThought());

        // ===== 5. 决策结束 =====
        if (decision.isFinished() || "END".equalsIgnoreCase(decision.getNextAgent())) {
            log.info("[Supervisor] 任务结束");
            return Map.of(
                    DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, StateGraph.END,
                    DataAgentSpec.Graph.StateKey.Planning.SUPERVISOR_ITERATION, iteration + 1
            );
        }

        // ===== 6. 把决策追加为一个新的 ExecutionStep，供 Sub-Agent 读取 =====
        Plan.ToolParameters params = new Plan.ToolParameters();
        params.setInstruction(decision.getTaskInstruction());
        params.setSummaryAndRecommendations(decision.getSummaryAndRecommendations());

        String nextTool = normalizeToolName(decision.getNextAgent());

        // 直接在内存对象上追加，避免再次读 state 旧 plan
        Plan.ExecutionStep newStep = new Plan.ExecutionStep();
        newStep.setStep(history.size() + 1);
        newStep.setToolToUse(nextTool);
        newStep.setToolParameters(params);
        history.add(newStep);
        String updatedPlanJson = objectMapper.writeValueAsString(currentPlan);

        // CURRENT_STEP 指向"本轮刚刚派出的这一步"，下游 Sub-Agent 通过 Plan.getCurrentStep 读到它。
        // SubAgent 不再修改 CURRENT_STEP，下一次回到 Supervisor 时由 Supervisor 自己重新写。
        int newCurrentStep = history.size();
        log.info("[Supervisor] 派发任务 -> {} | 步骤#{} | 指令: {}",
                nextTool, newCurrentStep, decision.getTaskInstruction());

        Map<String, Object> result = new HashMap<>();
        result.put(DataAgentSpec.Graph.StateKey.Planning.PLAN, updatedPlanJson);
        result.put(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, newCurrentStep);
        result.put(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, nextTool);
        result.put(DataAgentSpec.Graph.StateKey.Planning.SUPERVISOR_ITERATION, iteration + 1);
        return result;
    }

    /**
     * 构建已发生的 Agent 执行轨迹文本，给 Supervisor 观察用。
     * 包含每一步派给了谁、做了什么、产出是什么；以及 Sub-Agent 失败时归档的 error 摘要，
     * 让 Supervisor 在下一轮可以"看见失败"并重试或换路径。
     * <p>
     * 重要：当最后一步是失败步时，不附加该类型的"最近执行结果"，
     * 避免把上一轮成功的旧产物（state 中残留的 R1）当作本轮成功的证据误导 LLM。
     */
    @SuppressWarnings("unchecked")
    private String buildAgentTrace(OverAllState state, List<Plan.ExecutionStep> history) {
        if (history.isEmpty()) {
            return "（暂无历史，这是第一轮决策）";
        }
        StringBuilder sb = new StringBuilder();
        // 已知的最近一次产出（按节点类型）
        String lastSqlGen = state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, "");
        Object lastSqlExec = state.value(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, Object.class).orElse(null);
        String lastPyGen = state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_GENERATION_RESULT, "");
        Object lastPyExec = state.value(DataAgentSpec.Graph.StateKey.Execution.PYTHON_EXECUTION_RESULT, Object.class).orElse(null);

        // 各 Sub-Agent 写入的步骤产出（含成功 step_N、step_N_analysis、以及失败 step_N_error）
        Map<String, String> executionOutput = (Map<String, String>) state.value(
                DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, Object.class
        ).orElse(new HashMap<>());

        // 最后一步是否失败（用于决定是否附加"最近产出"）
        Plan.ExecutionStep lastStep = history.get(history.size() - 1);
        String lastStepErrKey = "step_" + history.size() + "_error";
        boolean lastStepFailed = executionOutput.containsKey(lastStepErrKey);
        String lastStepTool = lastStep.getToolToUse() == null ? "" : lastStep.getToolToUse();

        for (int i = 0; i < history.size(); i++) {
            Plan.ExecutionStep s = history.get(i);
            int stepNo = i + 1;
            sb.append("第").append(stepNo).append("步 -> 工具[").append(s.getToolToUse()).append("]");
            if (s.getToolParameters() != null && s.getToolParameters().getInstruction() != null) {
                sb.append(" | 指令: ").append(s.getToolParameters().getInstruction());
            }
            // 显式标注该步是否失败，让 Supervisor 直观看到
            String errKey = "step_" + stepNo + "_error";
            if (executionOutput.containsKey(errKey)) {
                sb.append(" | ❌ 失败: ").append(truncate(executionOutput.get(errKey), 600));
            }
            sb.append('\n');
        }

        // 附加最近一次的执行产出摘要（避免上下文过长，仅截断）。
        // 关键：只有最后一步成功时才展示该类型的"最近产出"，避免把上轮残留误判为本轮成果。
        boolean showSqlArtifacts = !(lastStepFailed
                && (DataAgentSpec.Graph.Node.SQL_GENERATION.equals(lastStepTool)));
        boolean showPyArtifacts = !(lastStepFailed
                && (DataAgentSpec.Graph.Node.PYTHON_GENERATION.equals(lastStepTool)));

        if (showSqlArtifacts && lastSqlGen != null && !lastSqlGen.isBlank()) {
            sb.append("[最近 SQL 生成]: ").append(truncate(lastSqlGen, 500)).append('\n');
        }
        if (showSqlArtifacts && lastSqlExec != null) {
            try {
                sb.append("[最近 SQL 执行结果]: ").append(truncate(objectMapper.writeValueAsString(lastSqlExec), 800)).append('\n');
            } catch (Exception ignored) {
            }
        }
        if (showPyArtifacts && lastPyGen != null && !lastPyGen.isBlank()) {
            sb.append("[最近 Python 代码]: ").append(truncate(lastPyGen, 400)).append('\n');
        }
        if (showPyArtifacts && lastPyExec != null) {
            try {
                sb.append("[最近 Python 执行结果]: ").append(truncate(objectMapper.writeValueAsString(lastPyExec), 800)).append('\n');
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    /**
     * 调用 LLM 做下一步决策。
     */
    private SupervisorDecision decideNextStep(String userQuestion, String agentTrace) {
        BeanOutputConverter<SupervisorDecision> converter = new BeanOutputConverter<>(SupervisorDecision.class);

        String prompt = String.format("""
                你是一个数据分析任务的 Supervisor（主管 Agent），负责调度多个 Sub-Agent 完成用户问题。

                # 用户原始问题
                %s

                # 已经发生的执行轨迹
                %s

                # 你可派遣的 Sub-Agent
                - SQL_GENERATION : 负责根据自然语言指令生成并执行 SQL，从数据库取数。
                - PYTHON_GENERATION : 负责对已有 SQL 结果集做进一步的数据分析/计算/绘图。必须在已有 SQL 结果之后才能调用。
                - REPORT_GENERATION : 把全部已收集的数据生成最终面向用户的报告。整个任务的最后一步必须是它。
                - END : 任务终止（异常/无法继续/已经生成报告）。

                # 决策原则
                1. 第一步通常是 SQL_GENERATION（先取数）。
                2. 仅在已有可用 SQL 结果时才派 PYTHON_GENERATION。
                3. 当数据已收集足够能回答用户问题时，派 REPORT_GENERATION 收尾，然后下一轮直接 finished=true。
                4. 不要无意义重复同一类操作。

                # 失败处理原则（重要）
                - 如果上一步在轨迹中标记了"❌ 失败"，请仔细阅读失败原因：
                  · SQL 失败（语法/字段不存在等）：可以再次派 SQL_GENERATION，并在 instruction 中**明确指出**上次的错误以及如何修正。
                  · Python 失败（代码错误/执行异常）：可以再次派 PYTHON_GENERATION 让它修正。
                  · 同一类失败**累计达到 2 次**就不要再重试，应该改派 REPORT_GENERATION 把已有数据汇总告知用户，或在彻底无数据时 finished=true 结束。
                - 注意区分"失败"和"成功但结果为空"：结果为空也可能是正常业务结果，不应反复重试。

                # 输出格式
                %s
                """,
                userQuestion,
                agentTrace,
                converter.getFormat()
        );

        log.info("[Supervisor] 决策提示词:\n{}", prompt);
        try {
            String content = deepseekClient
                    .prompt()
                    .options(OpenAiChatOptions.builder().extraBody(Map.of("enable_thinking", false)))
                    .user(prompt)
                    .call()
                    .content();
            SupervisorDecision decision = converter.convert(content);
            if (decision == null) {
                log.error("[Supervisor] 决策结果转换为 null，强制结束 content={}", content);
                return safeFallbackDecision("Supervisor 决策返回空，强制结束");
            }
            return decision;
        } catch (Exception ex) {
            // LLM 调用异常 / JSON 解析异常 → 安全兜底，避免整张图崩溃返回 500。
            // 此时让 Supervisor 直接走 END，前端会拿到一个明确的错误消息而不是接口层 5xx。
            log.error("[Supervisor] LLM 决策异常，安全兜底走 END, err={}", ex.getMessage(), ex);
            return safeFallbackDecision("Supervisor 决策异常: " + ex.getMessage());
        }
    }

    /**
     * 当 LLM 调用/解析失败时构造一个"安全终止"决策，避免整图崩溃。
     */
    private SupervisorDecision safeFallbackDecision(String reason) {
        SupervisorDecision fallback = new SupervisorDecision();
        fallback.setThought(reason);
        fallback.setNextAgent("END");
        fallback.setFinished(true);
        return fallback;
    }

    /**
     * 把 LLM 输出的 agent 名字归一到图节点名常量。
     */
    private String normalizeToolName(String agent) {
        if (agent == null) {
            return StateGraph.END;
        }
        String upper = agent.trim().toUpperCase();
        return switch (upper) {
            case "SQL", "SQL_GENERATION", "SQL_GENERATE_NODE" -> DataAgentSpec.Graph.Node.SQL_GENERATION;
            case "PYTHON", "PYTHON_GENERATION", "PYTHON_GENERATE_NODE" -> DataAgentSpec.Graph.Node.PYTHON_GENERATION;
            case "REPORT", "REPORT_GENERATION", "REPORT_GENERATOR_NODE" -> DataAgentSpec.Graph.Node.REPORT_GENERATION;
            default -> StateGraph.END;
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...(truncated)";
    }

    /**
     * Supervisor 单轮决策的输出结构。
     */
    @Data
    public static class SupervisorDecision {
        @JsonProperty("thought")
        @JsonPropertyDescription("简要描述你为什么这样决策")
        private String thought;

        @JsonProperty("next_agent")
        @JsonPropertyDescription("下一个要派遣的 Sub-Agent，可选值：SQL_GENERATION / PYTHON_GENERATION / REPORT_GENERATION / END")
        private String nextAgent;

        @JsonProperty("task_instruction")
        @JsonPropertyDescription("派给该 Sub-Agent 的任务指令文本（SQL/Python 节点会读取此字段）")
        private String taskInstruction;

        @JsonProperty("summary_and_recommendations")
        @JsonPropertyDescription("仅当 next_agent=REPORT_GENERATION 时填写，作为报告大纲；其他情况留空")
        private String summaryAndRecommendations;

        @JsonProperty("finished")
        @JsonPropertyDescription("是否整个任务已完成；当填 true 时图将走向 END")
        private boolean finished;
    }
}
