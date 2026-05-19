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
public class ToyWrapUpNode implements NodeAction {

    private final ChatClient chatClient;

    public ToyWrapUpNode(@Qualifier("deepseekClient") ChatClient deepseekClient) {
        this.chatClient = deepseekClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String sceneLabel = state.value(ToyGraphSpec.StateKey.SCENE_LABEL, "内容规划");
        String draft = state.value(ToyGraphSpec.StateKey.DRAFT, "");
        Flux<String> flux = chatClient
                .prompt()
                .system("你是一个教程里的收尾节点。请把输入整理成更通俗的行动清单，控制在 3 到 5 条，每条一句话。")
                .user("请把这份" + sceneLabel + "整理成用户一看就能执行的清单：" + draft)
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .stream()
                .content();
        return Map.of(ToyGraphSpec.StateKey.FINAL_OUTPUT, flux);
    }
}