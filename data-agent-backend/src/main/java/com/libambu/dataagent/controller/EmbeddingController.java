package com.libambu.dataagent.controller;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class EmbeddingController {
    
    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private VectorStore vectorStore;

    @GetMapping("/embedding")
    public float[] getEmbedding(String input) {
        float[] embedding = embeddingModel.embed(input);
        log.info("embedding: {}", Arrays.toString(embedding));
        return embedding;
    }

    @GetMapping("/embedding/compare")
    public Map<String, Object> compareEmbedding(String input1, String input2) {
        float[] embedding1 = embeddingModel.embed(input1);
        float[] embedding2 = embeddingModel.embed(input2);

        double cosineSimilarity = cosineSimilarity(embedding1, embedding2);
        double euclideanDistance = euclideanDistance(embedding1, embedding2);

        return Map.of(
                "input1", input1,
                "input2", input2,
                "dimension", embedding1.length,
                "cosineSimilarity", cosineSimilarity,
                "euclideanDistance", euclideanDistance
        );
    }

    private double cosineSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private double euclideanDistance(float[] vector1, float[] vector2) {
        double sum = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            double diff = vector1[i] - vector2[i];
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    @GetMapping("/vector/add")
    public Map<String, Object> addVector(@RequestParam(required = false) String text) {
        // 如果没有传 text，或者 text 是空字符串，就直接返回提示
        // 因为 Document 必须有文本内容，否则会报 exactly one of text or media must be specified
        if (text == null || text.isBlank()) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "text 参数不能为空，请使用 /vector/add?text=你要存入的内容");
            return result;
        }

        // 创建一个 Map，用来保存这条文档的附加信息
        // metadata 不参与主要文本内容展示，但会和文档一起存入 Redis 向量数据库
        Map<String, Object> metadata = new HashMap<>();

        // 给 metadata 添加一个 type 字段，表示这条数据是 demo 测试数据
        metadata.put("type", "demo");

        // 创建一条 Spring AI 文档对象
        // text 是用户传进来的文本内容，比如“苹果是一种水果”
        // metadata 是这条文本的额外信息
        Document document = new Document(text, metadata);

        // 把文档写入向量数据库
        // Spring AI 会自动完成两件事：  
        // 1. 调用 EmbeddingModel，把 text 转成向量
        // 2. 把原始文本、metadata 和向量一起存入 Redis
        vectorStore.add(List.of(document));

        // 创建返回给浏览器/Postman 的结果 Map
        Map<String, Object> result = new HashMap<>(); 

        // 返回提示信息，告诉调用方添加成功
        result.put("message", "添加成功");

        // 返回 Spring AI 自动生成的文档 id
        result.put("id", document.getId());

        // 返回刚刚存入的原始文本
        result.put("text", document.getText());

        // 返回这条文档的附加信息
        result.put("metadata", document.getMetadata());

        // 把结果返回给前端或者 Postman
        return result;
    }

    @GetMapping("/vector/search")
    public List<Map<String, Object>> searchVector(@RequestParam(required = false) String query,
            @RequestParam(required = false) Integer topK) {
        // 如果没有传 query，或者 query 是空字符串，就直接返回提示
        // 因为向量搜索必须有查询内容，Spring AI 才能把 query 转成向量去 Redis 里查相似数据
        if (query == null || query.isBlank()) {
            return List.of(Map.of("message", "query 参数不能为空，请使用 /vector/search?query=你要搜索的内容"));
        }

        // topK 表示最多返回几条相似结果
        // 如果调用接口时没有传 topK，就默认返回 3 条
        int limit = topK == null ? 3 : topK;

        // 构建一次向量搜索请求
        // query 是用户输入的搜索内容，比如“我想吃水果”
        SearchRequest searchRequest = SearchRequest.builder()
                // 设置本次搜索的查询文本
                // Spring AI 会自动把 query 转成向量
                .query(query)
                // 设置最多返回多少条相似结果
                .topK(limit)
                // 接受所有相似度结果，不额外设置最低相似度门槛
                // 学习测试阶段这样更容易看到返回结果
                .similarityThresholdAll()
                // 创建最终的 SearchRequest 对象
                .build();

        // 执行向量相似度搜索
        // Spring AI 会拿 query 的向量去 Redis 向量库中查找最相似的文档
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        // 把搜索出来的 Document 列表转换成更适合接口返回的 Map 列表
        return documents.stream().map(document -> {
            // 每一条搜索结果都封装成一个 Map
            Map<String, Object> item = new HashMap<>();

            // 返回文档 id
            item.put("id", document.getId());

            // 返回文档原始文本内容
            item.put("text", document.getText());

            // 返回相似度分数
            // 分数越接近 1，一般表示和 query 越相似
            item.put("score", document.getScore());

            // 返回文档附加信息
            item.put("metadata", document.getMetadata());

            // 返回当前这一条结果
            return item;
        }).toList();
    }


}
