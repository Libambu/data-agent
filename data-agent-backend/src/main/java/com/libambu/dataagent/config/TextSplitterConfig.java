package com.libambu.dataagent.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TextSplitterConfig {
    
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(50) // 设置每个文本块的最大 token 数量
                .withMaxNumChunks(1000) // 设置最大文本块数量，防止过多文本块导致内存问题
                .build();
    }

}
  