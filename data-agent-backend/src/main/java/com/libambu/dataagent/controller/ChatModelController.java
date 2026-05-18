package com.libambu.dataagent.controller;

import java.util.Map;

import com.libambu.dataagent.entity.pjo.ActorMovies;
import com.libambu.dataagent.tools.DataTimeTool;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;


@RestController
class ChatModelController {

    @Autowired
    @Qualifier("qwenClient")
    private ChatClient qwenClient;

    @Autowired
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

    @Autowired
    private DataTimeTool dataTimeTool;

    @GetMapping("/ai/qwen")
    String generation(String userInput) {
        return this.qwenClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/ai/deepseek")
    String generationByDs(String userInput,String convId) {
        return this.deepseekClient.prompt()
                .user(userInput)
                .tools(dataTimeTool)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, convId)) //在调用时动态传入conversationId参数，供MessageChatMemoryAdvisor使用
                .call()
                .content(); 
    } 

    //消息流式发送
    @GetMapping(value = "/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    Flux<String> generationStream(String userInput) {
        return this.deepseekClient.prompt()
                .user(userInput)
                .stream()
                .content();
    }
    //返回特定的结构体数据
    //结构体定义的字段要比提示词更加优先
    @GetMapping("/ai/json") 
    ActorMovies generationJson(String userInput) {
        return this.deepseekClient.prompt()
                .user(userInput)
                .call()
                .entity(ActorMovies.class);
    }

    //提示词模版
    @GetMapping(value = "/ai/template", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    Flux<String> PromptTemplate(String userInput) {
        //UserMessage级别
        PromptTemplate promptTemplate = new PromptTemplate("介绍一下{topic}的相关信息");
        Prompt prompt = promptTemplate.create(Map.of("topic", userInput));
        return this.deepseekClient
                .prompt(prompt)
                //.user(u->u.text("请用100字介绍一下这个话题{}").param("topic", userInput))template简洁写法
                .stream()
                .content();
    }

    @GetMapping("/ai/datetime")
    String dateTimeTool(String userInput,String convId) {
        return this.deepseekClient.prompt()
                .user(userInput)
                .tools(dataTimeTool)
                .advisors(a->a.param(ChatMemory.CONVERSATION_ID, convId)) //在调用时动态传入conversationId参数，供MessageChatMemoryAdvisor使用
                .call() 
                .content();
    } 
}
