package com.libambu.dataagent.agent.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Prompt 模板管理器。
 * <p>
 * 集中加载 classpath:/prompts/*.st 模板，避免在节点里散落 PromptTemplate 实例。
 * 当前仅暴露知识/Schema 召回环节使用的模板，其余可后续按需补齐。
 */
@Component
public class PromptManager {

    private final PromptTemplate evidenceQueryRewritePromptTemplate;
    private final PromptTemplate businessKnowledgePromptTemplate;
    private final PromptTemplate agentKnowledgePromptTemplate;
    private final PromptTemplate mixSelectorPromptTemplate;

    public PromptManager(
            @Value("classpath:/prompts/evidence-query-rewrite.st") Resource evidenceQueryRewriteResource,
            @Value("classpath:/prompts/business-knowledge.st") Resource businessKnowledgeResource,
            @Value("classpath:/prompts/agent-knowledge.st") Resource agentKnowledgeResource,
            @Value("classpath:/prompts/mix-selector.st") Resource mixSelectorResource
    ) {
        this.evidenceQueryRewritePromptTemplate = new PromptTemplate(evidenceQueryRewriteResource);
        this.businessKnowledgePromptTemplate = new PromptTemplate(businessKnowledgeResource);
        this.agentKnowledgePromptTemplate = new PromptTemplate(agentKnowledgeResource);
        this.mixSelectorPromptTemplate = new PromptTemplate(mixSelectorResource);
    }

    public PromptTemplate getEvidenceQueryRewritePromptTemplate() {
        return evidenceQueryRewritePromptTemplate;
    }

    public PromptTemplate getBusinessKnowledgePromptTemplate() {
        return businessKnowledgePromptTemplate;
    }

    public PromptTemplate getAgentKnowledgePromptTemplate() {
        return agentKnowledgePromptTemplate;
    }

    public PromptTemplate getMixSelectorPromptTemplate() {
        return mixSelectorPromptTemplate;
    }
}
