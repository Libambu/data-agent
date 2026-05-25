package com.libambu.dataagent.entity.constant;

/**
 * Data-Agent 规范常量。
 * <p>
 * 定义向量库 Document 的元数据 key 与 vectorType 取值，
 * 方便检索时按类型/数据库维度过滤。
 */
public final class DataAgentSpec {

    private DataAgentSpec() {
    }

    /** Graph 名称：StateGraph 注册时使用，用于日志/可视化区分不同图。 */
    public static final String GRAPH_NAME = "data-agent-main-graph";

    /** Graph 节点、状态键命名空间。 */
    public static final class Graph {

        private Graph() {
        }

        /** Graph 节点名。 */
        public static final class Node {
            private Node() {
            }

            public static final String EVIDENCE_RECALL = "EVIDENCE_RECALL_NODE";
            public static final String SCHEMA_RECALL = "SCHEME_RECALL_NODE";
            public static final String TABLE_RELATION = "TABLE_RELATION_NODE";
            public static final String FEASIBILITY_ASSESSMENT = "FEASIBILITY_ASSESSMENT_NODE";
            public static final String PLANNER = "PLANNER_NODE";
            public static final String HUMAN_FEEDBACK = "HUMAN_FEEDBACK_NODE";
            public static final String PLAN_EXECUTION = "PLAN_EXECUTE_NODE";
            public static final String SQL_GENERATION = "SQL_GENERATE_NODE";
            public static final String SQL_EXECUTION = "SQL_EXECUTE_NODE";
            public static final String PYTHON_GENERATION = "PYTHON_GENERATE_NODE";
            public static final String PYTHON_EXECUTION = "PYTHON_EXECUTE_NODE";
            public static final String PYTHON_ANALYSIS = "PYTHON_ANALYZE_NODE";
            public static final String REPORT_GENERATION = "REPORT_GENERATOR_NODE";
            /** 中断节点（人工审核节点） */
            public static final String INTERRUPT_NODE = HUMAN_FEEDBACK;
        }

        /** Graph 状态 Key。 */
        public static final class StateKey {
            private StateKey() {
            }

            /** 入参：图启动时由 A2A Executor 写入的初始状态。 */
            public static final class Input {
                private Input() {
                }

                /** 用户原始问题文本（来自 A2A Message 的 TextPart）。 */
                public static final String USER_INPUT = "input";
                /** 当前会话绑定的数据库 ID（来自 Message.metadata.databaseId），用于限定召回范围。 */
                public static final String DATABASE_ID = "databaseId";
                /** 多轮对话上下文（历史问答拼接），EvidenceRecallNode 中用于 query 改写参考。 */
                public static final String MULTI_TURN_CONTEXT = "MULTI_TURN_CONTEXT";
            }

            /** 召回阶段：证据/Schema/表关系等检索阶段产出的中间数据。 */
            public static final class Recall {
                private Recall() {
                }

                /** 经过 LLM 改写/扩展后的检索 query，由 EvidenceRecallNode 写入，被后续召回与 Planner 复用。 */
                public static final String REWRITE_QUERY = "REWRITE_QUERY";
                /** 证据召回结果：业务术语（GLOSSARY_KNOWLEDGE）+ 历史问答（QUESTION_KNOWLEDGE）拼接的上下文文本。 */
                public static final String EVIDENCE = "EVIDENCE";
                /** 列 Schema 召回结果（向量检索 vectorType=column 的 Document 列表），用于 SQL/Planner 参考。 */
                public static final String COLUMN_SCHEMA = "COLUMN_SCHEME";
                /** 表 Schema 召回结果（向量检索 vectorType=table 的 Document 列表），用于 SQL/Planner 参考。 */
                public static final String TABLE_SCHEMA = "TABLE_SCHEME";
                /** 表关系分析结果（外键/Join 关系等），由 TableRelationNode 输出，FeasibilityAssessment/Planner 读取。 */
                public static final String TABLE_RELATION = "TABLE_RELATION_OUTPUT";
            }

            /** 规划阶段：Planner 生成的执行计划，及其执行/校验/路由相关的状态。 */
            public static final class Planning {
                private Planning() {
                }

                /** Planner 节点产出的完整执行计划（结构化 Plan 对象），人工审核与 PlanExecuteNode 都会读取。 */
                public static final String PLAN = "PLANNER_NODE_OUTPUT";
                /** 计划校验失败时的错误信息，用于让 Planner 下一轮针对性修复。 */
                public static final String VALIDATION_ERROR = "PLAN_VALIDATION_ERROR";
                /** 计划被拒绝/校验失败的累计次数，HumanFeedbackNode 用它做熔断（>=3 时强制结束）。 */
                public static final String REPAIR_COUNT = "PLAN_REPAIR_COUNT";
                /** 规划/执行阶段写入的下一节点路由 key，配合条件边决定走向。 */
                public static final String NEXT_NODE = "PLAN_NEXT_NODE";
                /** 当前正在执行的计划步骤序号，PlanExecuteNode/Plan 中用来定位下一步要执行的子任务。 */
                public static final String CURRENT_STEP = "PLAN_CURRENT_STEP";
                /** 计划校验状态（通过/不通过等枚举标识），驱动 Planner 是否需要重新生成。 */
                public static final String VALIDATION_STATUS = "PLAN_VALIDATION_STATUS";
                /** PlanExecuteNode 执行完成后聚合的产出结果，作为最终回答/下游引用的数据。 */
                public static final String EXECUTION_OUTPUT = "PLAN_EXECUTE_NODE_OUTPUT";
            }

            /** 人工评审阶段：用户对计划的确认结果与审核后路由。 */
            public static final class HumanReview {
                private HumanReview() {
                }

                /** 用户是否批准当前计划（Boolean）。GraphAgentExecutor 从 Message.metadata 注入到 state 中。 */
                public static final String CONFIRMATION_APPROVED = "confirmationApproved";
                /** 用户拒绝时的反馈意见文本，被 PlannerNode 读取用于针对性修计划。 */
                public static final String CONFIRMATION_FEEDBACK = "confirmationFeedback";
                /** 人工审核后由 HumanFeedbackEdge 读取的下一节点（END / PLAN_EXECUTION / PLANNER）。 */
                public static final String NEXT_NODE = "HUMAN_NEXT_NODE";
            }

            /** 执行阶段：可行性评估、SQL/Python 生成执行、报告生成等节点的输出。 */
            public static final class Execution {
                private Execution() {
                }

                /** 可行性评估节点的输出（是否可行 + 理由），决定是否进入 Planner。 */
                public static final String FEASIBILITY_RESULT = "FEASIBILITY_ASSESSMENT_NODE_OUTPUT";
                /** SQL 生成节点的产出（生成的 SQL 文本及附属信息）。 */
                public static final String SQL_GENERATION_RESULT = "SQL_GENERATE_OUTPUT";
                /** SQL 执行节点的产出（查询结果集/异常信息）。 */
                public static final String SQL_EXECUTION_RESULT = "SQL_EXECUTE_OUTPUT";
                /** Python 代码生成节点的产出（生成的 Python 代码）。 */
                public static final String PYTHON_GENERATION_RESULT = "PYTHON_GENERATE_NODE_OUTPUT";
                /** Python 代码执行节点的产出（执行结果/标准输出/异常）。 */
                public static final String PYTHON_EXECUTION_RESULT = "PYTHON_EXECUTE_NODE_OUTPUT";
                /** 报告生成节点的最终产出（自然语言报告/可视化数据）。 */
                public static final String REPORT_RESULT = "REPORT_GENERATOR_NODE_OUTPUT";
            }
        }
    }

    /** 消息元数据 key：A2A Message.metadata 中前后端约定的字段名。 */
    public static final class MessageMetadataKey {
        private MessageMetadataKey() {
        }

        /** 前端发起任务时携带的目标数据库 ID。 */
        public static final String DATABASE_ID = "databaseId";
        /** 人工审核回传时表示是否批准计划（Boolean）。 */
        public static final String CONFIRMATION_APPROVED = "confirmationApproved";
        /** 人工审核回传时附带的反馈意见文本（拒绝场景使用）。 */
        public static final String CONFIRMATION_FEEDBACK = "confirmationFeedback";
    }

    /** 向量检索相关常量。 */
    public static final class Retrieval {

        private Retrieval() {
        }

        /** Document.metadata 中使用的 key：写入向量库时带上的过滤维度。 */
        public static final class DocumentMetadataKey {
            private DocumentMetadataKey() {
            }

            /** 表 ID，用于按表维度过滤召回。 */
            public static final String TABLE_ID = "tableId";
            /** 列 ID，用于按列维度过滤召回。 */
            public static final String COLUMN_ID = "columnId";
            /** 知识条目 ID（业务术语/历史问答的主键）。 */
            public static final String KNOWLEDGE_ID = "knowledgeId";
            /** 所属数据库 ID，限定检索范围避免跨库串扰。 */
            public static final String DATABASE_ID = "databaseId";
            /** 业务术语 ID（GLOSSARY 类型 Document 专用）。 */
            public static final String BUSINESS_TERM_ID = "businessTermId";
            /** 向量来源类型，取值见 {@link VectorType}。 */
            public static final String VECTOR_TYPE = "vectorType";
        }

        /** 向量类型，用于按数据来源过滤检索结果。 */
        public static final class VectorType {
            private VectorType() {
            }

            /** 历史问答知识：用户过去的问题及对应解决方案，证据召回阶段使用。 */
            public static final String QUESTION_KNOWLEDGE = "questionKnowledge";
            /** 业务术语/词典知识：业务名词到字段/含义的映射，证据召回阶段使用。 */
            public static final String GLOSSARY_KNOWLEDGE = "glossaryKnowledge";
            /** 列级 Schema 向量：字段的描述，Schema 召回时用。 */
            public static final String COLUMN = "column";
            /** 表级 Schema 向量：表的描述，Schema 召回时用。 */
            public static final String TABLE = "table";
        }
    }

    /** Prompt 模板名：与 resources 下的 prompt 文件名对应，PromptService 按名加载。 */
    public static final class PromptName {
        private PromptName() {
        }

        /** 意图识别：判断用户问题属于查询/分析/闲聊等类别。 */
        public static final String INTENT_RECOGNITION = "intent-recognition";
        /** 证据召回前的 query 改写：把口语化问题改写为利于向量检索的形式。 */
        public static final String EVIDENCE_QUERY_REWRITE = "evidence-query-rewrite";
        /** Agent 通用知识 prompt：注入 agent 角色与全局背景信息。 */
        public static final String AGENT_KNOWLEDGE = "agent-knowledge";
        /** 查询增强：基于召回到的证据进一步丰富/澄清用户原始问题。 */
        public static final String QUERY_ENHANCEMENT = "query-enhancement";
        /** 可行性评估：判断在当前 Schema/证据下问题是否可被回答。 */
        public static final String FEASIBILITY_ASSESSMENT = "feasibility-assessment";
        /** 混合选择器：在多种执行路径（SQL/Python 等）之间做选择。 */
        public static final String MIX_SELECTOR = "mix-selector";
        /** 语义一致性校验：检查 SQL/计划与用户问题的语义是否吻合。 */
        public static final String SEMANTIC_CONSISTENCY = "semantic-consistency";
        /** SQL 生成：基于召回 Schema + 证据生成最终 SQL。 */
        public static final String SQL_GENERATION = "new-sql-generate";
        /** 任务规划：拆解用户问题为有序的执行步骤（Plan）。 */
        public static final String PLANNER = "planner";
        /** 报告生成：把执行结果汇总为面向用户的自然语言报告。 */
        public static final String REPORT_GENERATION = "report-generator-plain";
        /** SQL 错误修复：执行报错时基于错误信息让 LLM 修复 SQL。 */
        public static final String SQL_ERROR_FIXER = "sql-error-fixer";
        /** Python 代码生成：用于数据分析/绘图等场景的代码生成。 */
        public static final String PYTHON_GENERATION = "python-generator";
        /** Python 结果分析：对 Python 执行结果进行解读/总结。 */
        public static final String PYTHON_ANALYSIS = "python-analyze";
        /** 业务知识注入：把业务术语/规则等领域知识喂给 LLM。 */
        public static final String BUSINESS_KNOWLEDGE = "business-knowledge";
        /** 语义模型：描述指标/维度等语义层信息。 */
        public static final String SEMANTIC_MODEL = "semantic-model";
        /** JSON 修复：当 LLM 输出非法 JSON 时调用此 prompt 让其修正。 */
        public static final String JSON_FIX = "json-fix";
        /** 数据视图分析：分析数据视图（dashboard/图表）相关的问题。 */
        public static final String DATA_VIEW_ANALYZE = "data-view-analyze";
    }
}
