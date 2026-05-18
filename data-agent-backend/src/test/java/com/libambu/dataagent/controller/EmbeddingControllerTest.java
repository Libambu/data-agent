package com.libambu.dataagent.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmbeddingController 单元测试
 * 测试 /vector/search 接口的各种场景
 */
@ExtendWith(MockitoExtension.class)
class EmbeddingControllerTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private EmbeddingController embeddingController;

    private Document mockDocument1;
    private Document mockDocument2;
    private Document mockDocument3;

    @BeforeEach
    void setUp() {
        // 创建模拟的 Document 对象，用于测试返回结果
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("type", "demo");
        mockDocument1 = new Document("苹果是一种水果", metadata1);
        mockDocument1.getMetadata().put("score", 0.95);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("type", "demo");
        mockDocument2 = new Document("香蕉是黄色的水果", metadata2);
        mockDocument2.getMetadata().put("score", 0.85);

        Map<String, Object> metadata3 = new HashMap<>();
        metadata3.put("type", "demo");
        mockDocument3 = new Document("橙子富含维生素C", metadata3);
        mockDocument3.getMetadata().put("score", 0.75);
    }

    @Test
    @DisplayName("测试 searchVector - query 为 null 时返回提示信息")
    void testSearchVector_QueryIsNull() {
        // 执行：调用方法，query 为 null，topK 为 null
        List<Map<String, Object>> result = embeddingController.searchVector(null, null);

        // 验证：返回包含提示信息的列表
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(1, result.size(), "应返回一条提示信息");
        assertEquals("query 参数不能为空，请使用 /vector/search?query=你要搜索的内容",
                result.get(0).get("message"), "提示信息应匹配");

        // 验证：vectorStore 不应被调用
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - query 为空字符串时返回提示信息")
    void testSearchVector_QueryIsEmpty() {
        // 执行：调用方法，query 为空字符串
        List<Map<String, Object>> result = embeddingController.searchVector("", null);

        // 验证：返回包含提示信息的列表
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(1, result.size(), "应返回一条提示信息");
        assertEquals("query 参数不能为空，请使用 /vector/search?query=你要搜索的内容",
                result.get(0).get("message"), "提示信息应匹配");

        // 验证：vectorStore 不应被调用
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - query 为空白字符串时返回提示信息")
    void testSearchVector_QueryIsBlank() {
        // 执行：调用方法，query 为空白字符串
        List<Map<String, Object>> result = embeddingController.searchVector("   ", null);

        // 验证：返回包含提示信息的列表
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(1, result.size(), "应返回一条提示信息");
        assertEquals("query 参数不能为空，请使用 /vector/search?query=你要搜索的内容",
                result.get(0).get("message"), "提示信息应匹配");

        // 验证：vectorStore 不应被调用
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - 正常搜索，提供 query 和 topK")
    void testSearchVector_WithQueryAndTopK() {
        // 准备：mock vectorStore 返回模拟文档列表
        List<Document> mockDocuments = List.of(mockDocument1, mockDocument2);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockDocuments);

        // 执行：调用方法，提供 query 和 topK
        List<Map<String, Object>> result = embeddingController.searchVector("水果", 2);

        // 验证：返回结果不为空
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(2, result.size(), "应返回 2 条结果");

        // 验证：第一条结果的内容
        Map<String, Object> firstResult = result.get(0);
        assertEquals(mockDocument1.getId(), firstResult.get("id"), "id 应匹配");
        assertEquals(mockDocument1.getText(), firstResult.get("text"), "text 应匹配");
        assertEquals(mockDocument1.getScore(), firstResult.get("score"), "score 应匹配");
        assertEquals(mockDocument1.getMetadata(), firstResult.get("metadata"), "metadata 应匹配");

        // 验证：第二条结果的内容
        Map<String, Object> secondResult = result.get(1);
        assertEquals(mockDocument2.getId(), secondResult.get("id"), "id 应匹配");
        assertEquals(mockDocument2.getText(), secondResult.get("text"), "text 应匹配");

        // 验证：vectorStore.similaritySearch 被调用一次
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - 只提供 query，topK 为 null 时使用默认值 3")
    void testSearchVector_QueryOnly_TopKDefault() {
        // 准备：mock vectorStore 返回 3 条模拟文档
        List<Document> mockDocuments = List.of(mockDocument1, mockDocument2, mockDocument3);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockDocuments);

        // 执行：调用方法，只提供 query，topK 为 null
        List<Map<String, Object>> result = embeddingController.searchVector("水果", null);

        // 验证：返回结果不为空
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(3, result.size(), "应使用默认 topK=3，返回 3 条结果");

        // 验证：所有结果都包含必要的字段
        for (Map<String, Object> item : result) {
            assertTrue(item.containsKey("id"), "结果应包含 id 字段");
            assertTrue(item.containsKey("text"), "结果应包含 text 字段");
            assertTrue(item.containsKey("score"), "结果应包含 score 字段");
            assertTrue(item.containsKey("metadata"), "结果应包含 metadata 字段");
        }

        // 验证：vectorStore.similaritySearch 被调用一次
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - 搜索返回空结果")
    void testSearchVector_EmptyResult() {
        // 准备：mock vectorStore 返回空列表
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        // 执行：调用方法
        List<Map<String, Object>> result = embeddingController.searchVector("不存在的内容", 5);

        // 验证：返回空列表
        assertNotNull(result, "返回结果不应为 null");
        assertTrue(result.isEmpty(), "应返回空列表");

        // 验证：vectorStore.similaritySearch 被调用一次
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - topK 为 1 时只返回一条结果")
    void testSearchVector_TopKIsOne() {
        // 准备：mock vectorStore 返回多条文档（即使返回多条，topK 会限制）
        List<Document> mockDocuments = List.of(mockDocument1);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(mockDocuments);

        // 执行：调用方法，topK=1
        List<Map<String, Object>> result = embeddingController.searchVector("水果", 1);

        // 验证：返回结果不为空
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(1, result.size(), "应只返回 1 条结果");

        // 验证：结果内容正确
        Map<String, Object> item = result.get(0);
        assertEquals(mockDocument1.getId(), item.get("id"), "id 应匹配");
        assertEquals(mockDocument1.getText(), item.get("text"), "text 应匹配");

        // 验证：vectorStore.similaritySearch 被调用一次
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
    }

    @Test
    @DisplayName("测试 searchVector - 返回结果的 metadata 不为空")
    void testSearchVector_ResultContainsMetadata() {
        // 准备：mock vectorStore 返回包含 metadata 的文档
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "demo");
        metadata.put("source", "test");
        Document docWithMetadata = new Document("测试文本", metadata);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(docWithMetadata));

        // 执行：调用方法
        List<Map<String, Object>> result = embeddingController.searchVector("测试", 3);

        // 验证：返回结果包含 metadata
        assertNotNull(result, "返回结果不应为 null");
        assertEquals(1, result.size(), "应返回 1 条结果");

        Map<String, Object> item = result.get(0);
        Map<String, Object> returnedMetadata = (Map<String, Object>) item.get("metadata");
        assertNotNull(returnedMetadata, "metadata 不应为 null");
        assertEquals("demo", returnedMetadata.get("type"), "metadata 中的 type 应匹配");
        assertEquals("test", returnedMetadata.get("source"), "metadata 中的 source 应匹配");
    }
}
