package com.libambu.dataagent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * 最小 Graph 链路演示节点。
 * 逻辑严格对齐 data-agent-backend-gradle 中的 ToyHelloNode.kt：
 *   - 从 OverAllState 读取 "input"
 *   - 用 ChatClient（这里复用项目里已有的 deepseekClient Bean）.stream().chatResponse() 拿到 Flux<ChatResponse>
 *   - 把整个 Flux 作为 "output" 放入 state
 *   - extraBody 透传 enable_thinking=false（与 gradle 完全一致）
 */
@Component
public class ToyHelloNode implements NodeAction {

    private final ChatClient chatClient;

    public ToyHelloNode(@Qualifier("deepseekClient") ChatClient deepseekClient) {
        this.chatClient = deepseekClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String input = state.value("input", "");

        // spring-ai 2.0.0-M5 的 .options(...) 接受 ChatOptions.Builder<?>，
        Flux<ChatResponse> flux = chatClient
                .prompt()
                .user(input)
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .stream()
                .chatResponse();

        //节点执行完以后，把新的数据写回 Graph 状态。节点不是直接返回字符串，而是返回一个 Map<String, Object>，以后如果有第二个节点，它就可以继续从 state 里读 output。
        Map<String, Object> result = new HashMap<>();
        result.put("output", flux);
        return result;
    }
}
