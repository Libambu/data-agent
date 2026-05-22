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

    /** Graph 名称 */
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
        }

        /** Graph 状态 Key。 */
        public static final class StateKey {
            private StateKey() {
            }

            /** 入参。 */
            public static final class Input {
                private Input() {
                }

                public static final String USER_INPUT = "input";
                public static final String DATABASE_ID = "databaseId";
                public static final String MULTI_TURN_CONTEXT = "MULTI_TURN_CONTEXT";
            }

            /** 召回阶段。 */
            public static final class Recall {
                private Recall() {
                }

                public static final String REWRITE_QUERY = "REWRITE_QUERY";
                public static final String EVIDENCE = "EVIDENCE";
                public static final String COLUMN_SCHEMA = "COLUMN_SCHEME";
                public static final String TABLE_SCHEMA = "TABLE_SCHEME";
                public static final String TABLE_RELATION = "TABLE_RELATION_OUTPUT";
            }

            /** 规划阶段。 */
            public static final class Planning {
                private Planning() {
                }

                public static final String PLAN = "PLANNER_NODE_OUTPUT";
                public static final String VALIDATION_ERROR = "PLAN_VALIDATION_ERROR";
                public static final String REPAIR_COUNT = "PLAN_REPAIR_COUNT";
                public static final String NEXT_NODE = "PLAN_NEXT_NODE";
                public static final String CURRENT_STEP = "PLAN_CURRENT_STEP";
                public static final String VALIDATION_STATUS = "PLAN_VALIDATION_STATUS";
                public static final String EXECUTION_OUTPUT = "PLAN_EXECUTE_NODE_OUTPUT";
            }

            /** 人工评审。 */
            public static final class HumanReview {
                private HumanReview() {
                }

                public static final String FEEDBACK = "HUMAN_FEEDBACK_NODE_OUTPUT";
                public static final String REVIEW_ENABLED = "HUMAN_REVIEW_ENABLED";
                public static final String NEXT_NODE = "HUMAN_NEXT_NODE";
            }

            /** 执行阶段。 */
            public static final class Execution {
                private Execution() {
                }

                public static final String FEASIBILITY_RESULT = "FEASIBILITY_ASSESSMENT_NODE_OUTPUT";
                public static final String SQL_GENERATION_RESULT = "SQL_GENERATE_OUTPUT";
                public static final String SQL_EXECUTION_RESULT = "SQL_EXECUTE_OUTPUT";
                public static final String PYTHON_GENERATION_RESULT = "PYTHON_GENERATE_NODE_OUTPUT";
                public static final String PYTHON_EXECUTION_RESULT = "PYTHON_EXECUTE_NODE_OUTPUT";
                public static final String REPORT_RESULT = "REPORT_GENERATOR_NODE_OUTPUT";
            }
        }
    }

    /** 消息元数据 key。 */
    public static final class MessageMetadataKey {
        private MessageMetadataKey() {
        }

        public static final String DATABASE_ID = "databaseId";
    }

    /** 向量检索相关常量。 */
    public static final class Retrieval {

        private Retrieval() {
        }

        /** Document.metadata 中使用的 key。 */
        public static final class DocumentMetadataKey {
            private DocumentMetadataKey() {
            }

            public static final String TABLE_ID = "tableId";
            public static final String COLUMN_ID = "columnId";
            public static final String KNOWLEDGE_ID = "knowledgeId";
            public static final String DATABASE_ID = "databaseId";
            public static final String BUSINESS_TERM_ID = "businessTermId";
            public static final String VECTOR_TYPE = "vectorType";
        }

        /** 向量类型，用于按数据来源过滤检索结果。 */
        public static final class VectorType {
            private VectorType() {
            }

            public static final String QUESTION_KNOWLEDGE = "questionKnowledge";
            public static final String GLOSSARY_KNOWLEDGE = "glossaryKnowledge";
            public static final String COLUMN = "column";
            public static final String TABLE = "table";
        }
    }

    /** Prompt 模板名。 */
    public static final class PromptName {
        private PromptName() {
        }

        public static final String INTENT_RECOGNITION = "intent-recognition";
        public static final String EVIDENCE_QUERY_REWRITE = "evidence-query-rewrite";
        public static final String AGENT_KNOWLEDGE = "agent-knowledge";
        public static final String QUERY_ENHANCEMENT = "query-enhancement";
        public static final String FEASIBILITY_ASSESSMENT = "feasibility-assessment";
        public static final String MIX_SELECTOR = "mix-selector";
        public static final String SEMANTIC_CONSISTENCY = "semantic-consistency";
        public static final String SQL_GENERATION = "new-sql-generate";
        public static final String PLANNER = "planner";
        public static final String REPORT_GENERATION = "report-generator-plain";
        public static final String SQL_ERROR_FIXER = "sql-error-fixer";
        public static final String PYTHON_GENERATION = "python-generator";
        public static final String PYTHON_ANALYSIS = "python-analyze";
        public static final String BUSINESS_KNOWLEDGE = "business-knowledge";
        public static final String SEMANTIC_MODEL = "semantic-model";
        public static final String JSON_FIX = "json-fix";
        public static final String DATA_VIEW_ANALYZE = "data-view-analyze";
    }
}
