package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表结构（Schema）召回节点。
 * <p>
 * 依赖前置 {@link EvidenceRecallNode} 写入的 REWRITE_QUERY，
 * 在向量库中分别按 vectorType=table / vectorType=column 做语义召回，
 * 把相关的表 Document 与列 Document 写回 state，供下游 Schema 选择 / SQL 生成节点使用。
 */
@Component
public class SchemeReCallNode implements NodeAction {

    private final VectorStore vectorStore;

    public SchemeReCallNode(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");

        List<Document> tableDocuments = retrieveTable(rewriteQuery, databaseId);
        List<Document> columnDocuments = retrieveColumn(rewriteQuery, databaseId);

        Map<String, Object> result = new HashMap<>();
        result.put(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, tableDocuments);
        result.put(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, columnDocuments);
        return result;
    }

    /** 按表粒度召回相关 Schema 描述。 */
    public List<Document> retrieveTable(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.TABLE),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(10)
                .build();
        return vectorStore.similaritySearch(request);
    }

    /** 按列粒度召回相关 Schema 描述。 */
    public List<Document> retrieveColumn(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.COLUMN),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(30)
                .build();
        return vectorStore.similaritySearch(request);
    }
}
