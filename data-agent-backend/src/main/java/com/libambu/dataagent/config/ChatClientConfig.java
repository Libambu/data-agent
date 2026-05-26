package com.libambu.dataagent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.libambu.dataagent.advisor.MySimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;


@Configuration
public class ChatClientConfig {

    @Autowired
    private VectorStore vectorStore;

    @Bean("qwenClient")
    public ChatClient qwenClient(OpenAiChatModel chatModel) {
        return ChatClient
                 .builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().model("qwen3.6-plus"))
                .build();
    }

    /**
     * 集成对话记忆和向量数据库的 ChatClient 配置，对话log
     * @param chatModel
     * @param chatMemory
     * @return
     */
    @Bean("deepseekClient")
    public ChatClient deepseekClient(OpenAiChatModel chatModel,ChatMemory chatMemory) {

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(
                    SearchRequest.builder()
                        .topK(3) // 设置返回最相关的3条文档
                        .similarityThreshold(0.6) // 设置相似度分数，只有相似度高于0.6的文档才会被返回
                        .build()
                ).build();


        return ChatClient
                .builder(chatModel)
                .defaultOptions(OpenAiChatOptions.builder().model("deepseek-v4-pro"))
                .defaultSystem("你是一个清纯女大学生，但是有时候喜欢生气。") //设置系统角色
                //拦截器模式，增强ai能力
                //.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(
                    new MySimpleLoggerAdvisor()
                    //MessageChatMemoryAdvisor.builder(chatMemory).build()
                    //qaAdvisor//简单的RAG检索
                ) //添加自定义的日志记录advisor
                .build(); 
    }
    //内存型
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(100) // 设置最大消息数为100
                .chatMemoryRepository(new InMemoryChatMemoryRepository()) // 使用内存存储库
                .build();
    }
}
 