package com.libambu.dataagent.agent;

import com.libambu.dataagent.entity.dataset.DbColumn;
import com.libambu.dataagent.entity.dataset.DbTable;
import com.libambu.dataagent.entity.dataset.GlossaryKnowledge;
import com.libambu.dataagent.entity.dataset.QuestionKnowledge;
import org.springframework.ai.document.Document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 把数据集实体转成 Spring AI 的 {@link Document}，供向量库写入。
 * <p>
 * 与 kt 版 io.github.qifan777.server.agent.DocumentExtensions 行为一致：
 * 文本部分提供给 embedding 模型，metadata 用于 FilterExpression 过滤。
 */
public final class DocumentMapper {

    private DocumentMapper() {
    }

    public static Document toDocument(GlossaryKnowledge knowledge) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                DataAgentSpec.Retrieval.VectorType.GLOSSARY_KNOWLEDGE);
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, knowledge.getDatabaseId());
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.BUSINESS_TERM_ID, knowledge.getId().toString());

        String text = "业务名词: " + knowledge.getTerm()
                + ", 说明: " + knowledge.getDescription()
                + ", 同义词: " + knowledge.getSynonyms();
        return new Document(text, meta);
    }

    public static Document toDocument(QuestionKnowledge knowledge) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                DataAgentSpec.Retrieval.VectorType.QUESTION_KNOWLEDGE);
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.KNOWLEDGE_ID, knowledge.getId().toString());
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, knowledge.getDatabaseId());
        return new Document(knowledge.getQuestion(), meta);
    }

    public static Document toDocument(DbTable table) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                DataAgentSpec.Retrieval.VectorType.TABLE);
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, table.getDatabaseId());
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID, table.getId().toString());
        return new Document(table.getDescription(), meta);
    }

    /**
     * 列文档化。要求 column.dbTable 已通过 join 填充，否则取不到 databaseId / tableId，
     * 这一点与 kt 版 fetcher{ allScalarFields(); dbTable{ allScalarFields() } } 的语义一致。
     */
    public static Document toDocument(DbColumn column) {
        DbTable owner = column.getDbTable();
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                DataAgentSpec.Retrieval.VectorType.COLUMN);
        if (owner != null) {
            meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, owner.getDatabaseId());
            UUID tid = owner.getId();
            if (tid != null) {
                meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID, tid.toString());
            }
        } else if (column.getTableId() != null) {
            meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID, column.getTableId().toString());
        }
        meta.put(DataAgentSpec.Retrieval.DocumentMetadataKey.COLUMN_ID, column.getId().toString());
        return new Document(column.getDescription(), meta);
    }
}
