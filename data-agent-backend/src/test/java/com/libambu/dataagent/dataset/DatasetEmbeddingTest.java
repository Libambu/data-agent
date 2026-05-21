package com.libambu.dataagent.dataset;

import com.libambu.dataagent.agent.DataAgentSpec;
import com.libambu.dataagent.agent.DocumentMapper;
import com.libambu.dataagent.mapper.DbColumnMapper;
import com.libambu.dataagent.mapper.DbTableMapper;
import com.libambu.dataagent.mapper.GlossaryKnowledgeMapper;
import com.libambu.dataagent.mapper.QuestionKnowledgeMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 数据集向量化测试，对齐 kt 版 DatasetEmbeddingTest。
 * 把 4 类实体（Table / Column / QuestionKnowledge / GlossaryKnowledge）
 * 转为 Document 后按 10 条 chunk 写入向量库（pgvector）。
 */
@Slf4j
@SpringBootTest
class DatasetEmbeddingTest {

    private static final String DATABASE_ID = "california_schools";
    private static final int CHUNK_SIZE = 10;

    @Autowired
    private DbTableMapper dbTableMapper;
    @Autowired
    private DbColumnMapper dbColumnMapper;
    @Autowired
    private QuestionKnowledgeMapper questionKnowledgeMapper;
    @Autowired
    private GlossaryKnowledgeMapper glossaryKnowledgeMapper;
    @Autowired
    private VectorStore vectorStore;

    @Test
    void embeddingTest() {
        List<Document> documents = new ArrayList<>();
        dbTableMapper.findByDatabaseId(DATABASE_ID)
                .forEach(t -> documents.add(DocumentMapper.toDocument(t)));
        dbColumnMapper.findByDatabaseId(DATABASE_ID)
                .forEach(c -> documents.add(DocumentMapper.toDocument(c)));
        questionKnowledgeMapper.findByDatabaseId(DATABASE_ID)
                .forEach(q -> documents.add(DocumentMapper.toDocument(q)));
        glossaryKnowledgeMapper.findByDatabaseId(DATABASE_ID)
                .forEach(g -> documents.add(DocumentMapper.toDocument(g)));

        log.info("总共要向量化 {} 条文档", documents.size());
        for (int i = 0; i < documents.size(); i += CHUNK_SIZE) {
            int to = Math.min(i + CHUNK_SIZE, documents.size());
            vectorStore.add(documents.subList(i, to));
        }
    }

    @Test
    void retrieveTest() {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var filterExpression = builder.and(
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.TABLE),
                builder.eq(
                        DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID,
                        DATABASE_ID)
        ).build();

        SearchRequest request = SearchRequest.builder()
                .query("What is the highest eligible free rate for K-12 students in the schools in Alameda County?")
                .filterExpression(filterExpression)
                .topK(5)
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents != null) {
            documents.sort(Comparator.comparing(Document::getScore, Comparator.nullsLast(Comparator.reverseOrder())));
        }
        log.info("similaritySearch result: {}", documents);
    }
}
