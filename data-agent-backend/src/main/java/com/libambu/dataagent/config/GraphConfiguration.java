package com.libambu.dataagent.config;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.libambu.dataagent.agent.edges.FeasibilityAssessmentEdge;
import com.libambu.dataagent.agent.edges.HumanFeedbackEdge;
import com.libambu.dataagent.agent.edges.PlanExecutorEdge;
import com.libambu.dataagent.agent.nodes.EvidenceRecallNode;
import com.libambu.dataagent.agent.nodes.FeasibilityAssessmentNode;
import com.libambu.dataagent.agent.nodes.HumanFeedbackNode;
import com.libambu.dataagent.agent.nodes.PlanExecuteNode;
import com.libambu.dataagent.agent.nodes.PlannerNode;
import com.libambu.dataagent.agent.nodes.SchemeReCallNode;
import com.libambu.dataagent.agent.nodes.SqlExecuteNode;
import com.libambu.dataagent.agent.nodes.SqlGeneratorNode;
import com.libambu.dataagent.agent.nodes.TableRelationNode;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Agent 图配置。
 */
@Configuration
public class GraphConfiguration {

    /**
     * 数据 Agent 主链路 Graph：
     * START -> EVIDENCE_RECALL_NODE -> SCHEME_RECALL_NODE -> TABLE_RELATION_NODE
     *       -> FEASIBILITY_ASSESSMENT_NODE -> PLANNER_NODE -> HUMAN_FEEDBACK_NODE
     *       -> PLAN_EXECUTE_NODE -> (conditional) -> SQL_GENERATE_NODE -> SQL_EXECUTE_NODE -> END
     * <p>
     * 对齐 kt 版的召回、可行性评估、任务拆解、人工审核、计划执行与 SQL 生成/执行编排。
     */
    @Bean
    public StateGraph dataAgentMainGraph(EvidenceRecallNode evidenceRecallNode,
                                         SchemeReCallNode schemeReCallNode,
                                         TableRelationNode tableRelationNode,
                                         FeasibilityAssessmentNode feasibilityAssessmentNode,
                                         PlannerNode plannerNode,
                                         HumanFeedbackNode humanFeedbackNode,
                                         PlanExecuteNode planExecuteNode,
                                         SqlGeneratorNode sqlGeneratorNode,
                                         SqlExecuteNode sqlExecuteNode) throws Exception {
        KeyStrategyFactory keyStrategyFactory = () -> {
            Map<String, KeyStrategy> map = new HashMap<>();
            // ===== 入参阶段：用户原始输入与会话上下文 =====
            map.put(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, KeyStrategy.REPLACE);          // 用户输入的原始问题文本
            map.put(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, KeyStrategy.REPLACE);         // 当前会话绑定的数据库 ID（来自消息 metadata）
            map.put(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, KeyStrategy.REPLACE);  // 多轮对话上下文（历史问答）

            // ===== 召回阶段：证据/表/列/关系等检索结果 =====
            map.put(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, KeyStrategy.REPLACE);      // 经过改写/扩展后的检索 query
            map.put(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, KeyStrategy.REPLACE);           // 证据召回结果（业务知识/术语等）
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, KeyStrategy.REPLACE);       // 召回的表 Schema 列表
            map.put(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, KeyStrategy.REPLACE);      // 召回的列 Schema 列表
            map.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, KeyStrategy.REPLACE);     // 表关系分析结果（外键/Join 关系等）

            // ===== 规划阶段：Planner 生成的执行计划及流转控制 =====
            map.put(DataAgentSpec.Graph.StateKey.Planning.PLAN, KeyStrategy.REPLACE);             // Planner 输出的完整执行计划
            map.put(DataAgentSpec.Graph.StateKey.Planning.REPAIR_COUNT, KeyStrategy.REPLACE);     // 计划被拒绝/修复的次数（达到上限会熔断）
            map.put(DataAgentSpec.Graph.StateKey.Planning.NEXT_NODE, KeyStrategy.REPLACE);        // 规划阶段写入的下一节点路由
            map.put(DataAgentSpec.Graph.StateKey.Planning.CURRENT_STEP, KeyStrategy.REPLACE);     // 当前正在执行的计划步骤
            map.put(DataAgentSpec.Graph.StateKey.Planning.EXECUTION_OUTPUT, KeyStrategy.REPLACE); // 计划执行节点的产出结果

            // ===== 人工审核阶段：用户对计划的确认结果与路由 =====
            map.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_APPROVED, KeyStrategy.REPLACE); // 用户是否批准计划（true/false）
            map.put(DataAgentSpec.Graph.StateKey.HumanReview.CONFIRMATION_FEEDBACK, KeyStrategy.REPLACE); // 用户拒绝时给出的反馈意见，用于 Planner 重规划
            map.put(DataAgentSpec.Graph.StateKey.HumanReview.NEXT_NODE, KeyStrategy.REPLACE);            // 审核后由条件边读取的下一节点（PLAN_EXECUTION/PLANNER/END）

            // ===== 执行阶段：可行性评估、SQL 生成/执行等节点的结果 =====
            map.put(DataAgentSpec.Graph.StateKey.Execution.FEASIBILITY_RESULT, KeyStrategy.REPLACE);      // 可行性评估节点的输出结果
            map.put(DataAgentSpec.Graph.StateKey.Execution.SQL_GENERATION_RESULT, KeyStrategy.REPLACE);   // SQL 生成节点的产出
            map.put(DataAgentSpec.Graph.StateKey.Execution.SQL_EXECUTION_RESULT, KeyStrategy.REPLACE);    // SQL 执行节点的产出
            return map;
        };

        return new StateGraph(DataAgentSpec.GRAPH_NAME, keyStrategyFactory)
                .addNode(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, AsyncNodeAction.node_async(evidenceRecallNode))
                .addNode(DataAgentSpec.Graph.Node.SCHEMA_RECALL, AsyncNodeAction.node_async(schemeReCallNode))
                .addNode(DataAgentSpec.Graph.Node.TABLE_RELATION, AsyncNodeAction.node_async(tableRelationNode))
                .addNode(DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT, AsyncNodeAction.node_async(feasibilityAssessmentNode))
                .addNode(DataAgentSpec.Graph.Node.PLANNER, AsyncNodeAction.node_async(plannerNode))
                .addNode(DataAgentSpec.Graph.Node.HUMAN_FEEDBACK, AsyncNodeAction.node_async(humanFeedbackNode))
                .addNode(DataAgentSpec.Graph.Node.PLAN_EXECUTION, AsyncNodeAction.node_async(planExecuteNode))
                .addNode(DataAgentSpec.Graph.Node.SQL_GENERATION, AsyncNodeAction.node_async(sqlGeneratorNode))
                .addNode(DataAgentSpec.Graph.Node.SQL_EXECUTION, AsyncNodeAction.node_async(sqlExecuteNode))
                .addEdge(StateGraph.START, DataAgentSpec.Graph.Node.EVIDENCE_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.EVIDENCE_RECALL, DataAgentSpec.Graph.Node.SCHEMA_RECALL)
                .addEdge(DataAgentSpec.Graph.Node.SCHEMA_RECALL, DataAgentSpec.Graph.Node.TABLE_RELATION)
                .addEdge(DataAgentSpec.Graph.Node.TABLE_RELATION, DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT)
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.FEASIBILITY_ASSESSMENT,
                        AsyncEdgeAction.edge_async(new FeasibilityAssessmentEdge()),
                        Map.of(
                                DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.PLANNER,
                                StateGraph.END, StateGraph.END
                        )
                )
                .addEdge(DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.HUMAN_FEEDBACK)
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.HUMAN_FEEDBACK,
                        AsyncEdgeAction.edge_async(new HumanFeedbackEdge()),
                        Map.of(
                                StateGraph.END, StateGraph.END,
                                DataAgentSpec.Graph.Node.PLAN_EXECUTION, DataAgentSpec.Graph.Node.PLAN_EXECUTION,
                                DataAgentSpec.Graph.Node.PLANNER, DataAgentSpec.Graph.Node.PLANNER
                        )
                )
                .addConditionalEdges(
                        DataAgentSpec.Graph.Node.PLAN_EXECUTION,
                        AsyncEdgeAction.edge_async(new PlanExecutorEdge()),
                        Map.of(
                                DataAgentSpec.Graph.Node.SQL_GENERATION, DataAgentSpec.Graph.Node.SQL_GENERATION,
                                StateGraph.END, StateGraph.END
                        )
                )
                .addEdge(DataAgentSpec.Graph.Node.SQL_GENERATION, DataAgentSpec.Graph.Node.SQL_EXECUTION)
                .addEdge(DataAgentSpec.Graph.Node.SQL_EXECUTION, StateGraph.END);
    }
}
