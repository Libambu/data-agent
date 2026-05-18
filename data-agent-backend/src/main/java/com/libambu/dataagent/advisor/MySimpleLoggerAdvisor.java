package com.libambu.dataagent.advisor;

import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
/**
 * 实现call adviser 阻塞模式
 * 实现stream adviser 非阻塞模式
 * 通过advisor增强ai能力
 * 这个类是一个简单的日志记录advisor，记录请求和响应的日志
 */
@Slf4j
public class MySimpleLoggerAdvisor implements CallAdvisor,StreamAdvisor {

	@Override
	public String getName() { 
		return "MySimpleLoggerAdvisor";
	}

    /**
     * 用于Adviseor排序，数值越小优先级越高，用于Advisors链
     */
	@Override
	public int getOrder() { 
		return 0;
	}


	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
		logRequest(chatClientRequest);

		ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

		logResponse(chatClientResponse);

		return chatClientResponse;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
			StreamAdvisorChain streamAdvisorChain) {
		logRequest(chatClientRequest);

		Flux<ChatClientResponse> chatClientResponses = streamAdvisorChain.nextStream(chatClientRequest);

		return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponses, this::logResponse); 
	}

	private void logRequest(ChatClientRequest request) {
		log.debug("request: {}", request);
	}

	private void logResponse(ChatClientResponse chatClientResponse) {
		log.debug("response: {}", chatClientResponse);
	}

}
