package com.libambu.dataagent.agent.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Prompt 模板管理器。
 * <p>
 * 集中加载 classpath:/prompts/*.st 模板，避免在节点里散落 PromptTemplate 实例。
 * 与官方 kt 版本保持同一套 prompt 暴露面，便于各节点按名称复用。
 */
@Component
public class PromptManager {

    private final PromptTemplate intentRecognitionPromptTemplate;
    private final PromptTemplate evidenceQueryRewritePromptTemplate;
    private final PromptTemplate agentKnowledgePromptTemplate;
    private final PromptTemplate queryEnhancementPromptTemplate;
    private final PromptTemplate feasibilityAssessmentPromptTemplate;
    private final PromptTemplate mixSelectorPromptTemplate;
    private final PromptTemplate semanticConsistencyPromptTemplate;
    private final PromptTemplate newSqlGeneratorPromptTemplate;
    private final PromptTemplate plannerPromptTemplate;
    private final PromptTemplate reportGeneratorPlainPromptTemplate;
    private final PromptTemplate sqlErrorFixerPromptTemplate;
    private final PromptTemplate pythonGeneratorPromptTemplate;
    private final PromptTemplate pythonAnalyzePromptTemplate;
    private final PromptTemplate businessKnowledgePromptTemplate;
    private final PromptTemplate semanticModelPromptTemplate;
    private final PromptTemplate dataViewAnalyzePromptTemplate;

    public PromptManager(
            @Value("classpath:/prompts/intent-recognition.st") Resource intentRecognitionResource,
            @Value("classpath:/prompts/evidence-query-rewrite.st") Resource evidenceQueryRewriteResource,
            @Value("classpath:/prompts/agent-knowledge.st") Resource agentKnowledgeResource,
            @Value("classpath:/prompts/query-enhancement.st") Resource queryEnhancementResource,
            @Value("classpath:/prompts/feasibility-assessment.st") Resource feasibilityAssessmentResource,
            @Value("classpath:/prompts/mix-selector.st") Resource mixSelectorResource,
            @Value("classpath:/prompts/semantic-consistency.st") Resource semanticConsistencyResource,
            @Value("classpath:/prompts/new-sql-generate.st") Resource newSqlGeneratorResource,
            @Value("classpath:/prompts/planner.st") Resource plannerResource,
            @Value("classpath:/prompts/report-generator-plain.st") Resource reportGeneratorPlainResource,
            @Value("classpath:/prompts/sql-error-fixer.st") Resource sqlErrorFixerResource,
            @Value("classpath:/prompts/python-generator.st") Resource pythonGeneratorResource,
            @Value("classpath:/prompts/python-analyze.st") Resource pythonAnalyzeResource,
            @Value("classpath:/prompts/business-knowledge.st") Resource businessKnowledgeResource,
            @Value("classpath:/prompts/semantic-model.st") Resource semanticModelResource,
            @Value("classpath:/prompts/data-view-analyze.st") Resource dataViewAnalyzeResource
    ) {
        this.intentRecognitionPromptTemplate = new PromptTemplate(intentRecognitionResource);
        this.evidenceQueryRewritePromptTemplate = new PromptTemplate(evidenceQueryRewriteResource);
        this.agentKnowledgePromptTemplate = new PromptTemplate(agentKnowledgeResource);
        this.queryEnhancementPromptTemplate = new PromptTemplate(queryEnhancementResource);
        this.feasibilityAssessmentPromptTemplate = new PromptTemplate(feasibilityAssessmentResource);
        this.mixSelectorPromptTemplate = new PromptTemplate(mixSelectorResource);
        this.semanticConsistencyPromptTemplate = new PromptTemplate(semanticConsistencyResource);
        this.newSqlGeneratorPromptTemplate = new PromptTemplate(newSqlGeneratorResource);
        this.plannerPromptTemplate = new PromptTemplate(plannerResource);
        this.reportGeneratorPlainPromptTemplate = new PromptTemplate(reportGeneratorPlainResource);
        this.sqlErrorFixerPromptTemplate = new PromptTemplate(sqlErrorFixerResource);
        this.pythonGeneratorPromptTemplate = new PromptTemplate(pythonGeneratorResource);
        this.pythonAnalyzePromptTemplate = new PromptTemplate(pythonAnalyzeResource);
        this.businessKnowledgePromptTemplate = new PromptTemplate(businessKnowledgeResource);
        this.semanticModelPromptTemplate = new PromptTemplate(semanticModelResource);
        this.dataViewAnalyzePromptTemplate = new PromptTemplate(dataViewAnalyzeResource);
    }

    public PromptTemplate getIntentRecognitionPromptTemplate() {
        return intentRecognitionPromptTemplate;
    }

    public PromptTemplate getEvidenceQueryRewritePromptTemplate() {
        return evidenceQueryRewritePromptTemplate;
    }

    public PromptTemplate getAgentKnowledgePromptTemplate() {
        return agentKnowledgePromptTemplate;
    }

    public PromptTemplate getQueryEnhancementPromptTemplate() {
        return queryEnhancementPromptTemplate;
    }

    public PromptTemplate getFeasibilityAssessmentPromptTemplate() {
        return feasibilityAssessmentPromptTemplate;
    }

    public PromptTemplate getMixSelectorPromptTemplate() {
        return mixSelectorPromptTemplate;
    }

    public PromptTemplate getSemanticConsistencyPromptTemplate() {
        return semanticConsistencyPromptTemplate;
    }

    public PromptTemplate getNewSqlGeneratorPromptTemplate() {
        return newSqlGeneratorPromptTemplate;
    }

    public PromptTemplate getPlannerPromptTemplate() {
        return plannerPromptTemplate;
    }

    public PromptTemplate getReportGeneratorPlainPromptTemplate() {
        return reportGeneratorPlainPromptTemplate;
    }

    public PromptTemplate getSqlErrorFixerPromptTemplate() {
        return sqlErrorFixerPromptTemplate;
    }

    public PromptTemplate getPythonGeneratorPromptTemplate() {
        return pythonGeneratorPromptTemplate;
    }

    public PromptTemplate getPythonAnalyzePromptTemplate() {
        return pythonAnalyzePromptTemplate;
    }

    public PromptTemplate getBusinessKnowledgePromptTemplate() {
        return businessKnowledgePromptTemplate;
    }

    public PromptTemplate getSemanticModelPromptTemplate() {
        return semanticModelPromptTemplate;
    }

    public PromptTemplate getDataViewAnalyzePromptTemplate() {
        return dataViewAnalyzePromptTemplate;
    }
}
