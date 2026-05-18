package com.libambu.dataagent.controller;


import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.libambu.dataagent.utils.DocumentParseUtil;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class MyRagController {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentParseUtil documentParseUtil;

    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @RequestMapping("/addDocs")
    public String addDocs() {
        addChunkedDocuments("/Users/yulong/yulongWorkSpace/spring-ai-demo/data-agent-backend/springAl2.0.pdf");
        return "Documents added successfully.";
    }

    private void addChunkedDocuments(String filePath) {
        List<Document> documents = documentParseUtil.parse(filePath);
        List<Document> chunkedDocuments = tokenTextSplitter.apply(documents);

        // DashScope 的 text-embedding-v4 一次 embedding 请求最多只能处理 10 条文本。
        // 如果一次性把所有切片传给 vectorStore.add(...)，底层会一次批量生成向量，容易触发 batch size 超限。
        int batchSize = 10;
        for (int i = 0; i < chunkedDocuments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, chunkedDocuments.size());

            // subList 返回的是原列表视图，这里复制成新的 ArrayList，避免底层实现对视图列表兼容性不好。
            List<Document> batchDocuments = new ArrayList<>(chunkedDocuments.subList(i, end));
            vectorStore.add(batchDocuments);
        }

        log.info("Added {} chunked documents for file: {}", chunkedDocuments.size(), filePath);
    }

}
