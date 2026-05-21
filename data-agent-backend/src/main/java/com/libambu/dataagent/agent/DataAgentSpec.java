package com.libambu.dataagent.agent;

/**
 * Data-Agent 规范常量。
 * <p>
 * 与 kt 版 io.github.qifan777.server.agent.DataAgentSpec 保持完全一致：
 * 定义向量库 Document 的元数据 key 与 vectorType 取值，方便检索时按类型/数据库维度过滤。
 */
public final class DataAgentSpec {

    private DataAgentSpec() {
    }

    /** Graph 名称（与 kt 版保持一致） */
    public static final String GRAPH_NAME = "data-agent-main-graph";

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
}
