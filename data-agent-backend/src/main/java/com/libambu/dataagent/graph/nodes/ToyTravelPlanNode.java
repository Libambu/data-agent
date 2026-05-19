package com.libambu.dataagent.graph.nodes;

import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.graph.ToyGraphSpec;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ToyTravelPlanNode implements NodeAction {

    private final ChatClient chatClient;

    public ToyTravelPlanNode(@Qualifier("deepseekClient") ChatClient deepseekClient) {
        this.chatClient = deepseekClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String input = state.value(ToyGraphSpec.StateKey.INPUT, "");
        StringBuilder draftBuilder = new StringBuilder();
        Flux<?> flux = chatClient
                .prompt()
                .system("你是一个适合教程演示的旅行助手。输出要口语化、结构清晰，分成目的地亮点、半日安排、注意事项三部分。")
                .user("请根据这段需求给我一个简短旅行攻略：" + input)
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .stream()
                .content()
                .doOnNext(draftBuilder::append)
                .map(chunk -> (Object) chunk)
                .concatWith(Mono.fromSupplier(() -> GraphResponse.done(
                        Map.of(ToyGraphSpec.StateKey.DRAFT, draftBuilder.toString()))));
        return Map.of(ToyGraphSpec.StateKey.DRAFT, flux);
    }
}