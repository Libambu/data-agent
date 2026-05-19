package com.libambu.dataagent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

@Component
public class ToyStudyPlanNode implements NodeAction {

    private final ChatClient chatClient;

    public ToyStudyPlanNode(@Qualifier("deepseekClient") ChatClient deepseekClient) {
        this.chatClient = deepseekClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String input = state.value(ToyGraphSpec.StateKey.INPUT, "");
        Flux<String> flux = chatClient
                .prompt()
                .system("你是一个适合教程演示的学习教练。输出要口语化、结构清晰，分成目标拆解、今日安排、避坑提醒三部分。")
                .user("请根据这段需求给我一个简短学习计划：" + input)
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .stream()
                .content();
        return Map.of(ToyGraphSpec.StateKey.DRAFT, flux);
    }
}