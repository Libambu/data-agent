package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dataset.QuestionKnowledge;
import com.libambu.dataagent.entity.dto.EvidenceQueryRewriteDTO;
import com.libambu.dataagent.mapper.QuestionKnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 知识召回节点。
 * <p>
 * 流程：
 * <ol>
 *     <li>用 evidence-query-rewrite 提示词把多轮上下文 + 最新输入重写为独立完整问题；</li>
 *     <li>用重写后的 query 在向量库分别召回业务术语（GLOSSARY_KNOWLEDGE）与历史问答（QUESTION_KNOWLEDGE）；</li>
 *     <li>历史问答只在向量库里存了 question，回库（QuestionKnowledgeMapper.findByIds）拿到完整 Q&A；</li>
 *     <li>分别用 business-knowledge / agent-knowledge 模板渲染成最终的 Evidence 文本，写回 state。</li>
 * </ol>
 */
@Component
public class EvidenceRecallNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceRecallNode.class);

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final PromptManager promptManager;

    public EvidenceRecallNode(ChatModel chatModel,
                              VectorStore vectorStore,
                              QuestionKnowledgeMapper questionKnowledgeMapper,
                              PromptManager promptManager) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.questionKnowledgeMapper = questionKnowledgeMapper;
        this.promptManager = promptManager;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String userInput = state.value(DataAgentSpec.Graph.StateKey.Input.USER_INPUT, "");
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");
        String multiTurn = state.value(DataAgentSpec.Graph.StateKey.Input.MULTI_TURN_CONTEXT, "(无)");

        BeanOutputConverter<EvidenceQueryRewriteDTO> beanOutputConverter =
                new BeanOutputConverter<>(EvidenceQueryRewriteDTO.class);

        Map<String, Object> rewriteVars = new LinkedHashMap<>();
        rewriteVars.put("latest_query", userInput);
        rewriteVars.put("format", beanOutputConverter.getFormat());
        rewriteVars.put("multi_turn", multiTurn);
        String rewritePrompt = promptManager.getEvidenceQueryRewritePromptTemplate().render(rewriteVars);
        logger.info("Rewrite prompt: {}", rewritePrompt);

        String rewriteResponse = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .user(rewritePrompt)
                .call()
                .content();
        if (rewriteResponse == null || rewriteResponse.isBlank()) {
            throw new IllegalArgumentException("Invalid rewrite response");
        }
        logger.info("Rewrite response: {}", rewriteResponse);

        EvidenceQueryRewriteDTO convert = beanOutputConverter.convert(rewriteResponse);
        if (convert == null) {
            throw new IllegalArgumentException("Invalid rewrite response");
        }
        String rewriteQuery = convert.getStandaloneQuery();

        List<Document> terms = retrieveGlossaryKnowledge(rewriteQuery, databaseId);
        List<Document> knowledgeDocs = retrieveKnowledge(rewriteQuery, databaseId);

        String glossaryKnowledgeText = terms.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        List<UUID> ids = knowledgeDocs.stream()
                .map(d -> uuidOrNull(d.getMetadata(), DataAgentSpec.Retrieval.DocumentMetadataKey.KNOWLEDGE_ID))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
        int invalidKnowledgeIdCount = knowledgeDocs.size() - ids.size();
        if (invalidKnowledgeIdCount > 0) {
            logger.warn("Skipped {} recalled knowledge docs due to invalid knowledgeId metadata", invalidKnowledgeIdCount);
        }

        String questionKnowledgeText = "";
        if (!ids.isEmpty()) {
            List<QuestionKnowledge> questions = questionKnowledgeMapper.findByIds(ids);
            questionKnowledgeText = questions.stream()
                    .map(it -> "来源：" + it.getDatabaseId() + " Q: " + it.getQuestion() + " A: " + it.getAnswer())
                    .collect(Collectors.joining("\n"));
        }

        String glossaryPrompt = promptManager.getBusinessKnowledgePromptTemplate().render(Map.of(
                "businessKnowledge", glossaryKnowledgeText.isEmpty() ? "无" : glossaryKnowledgeText
        ));
        String knowledgePrompt = promptManager.getAgentKnowledgePromptTemplate().render(Map.of(
                "agentKnowledge", questionKnowledgeText.isEmpty() ? "无" : questionKnowledgeText
        ));

        String evidence;
        if (questionKnowledgeText.isEmpty() && glossaryKnowledgeText.isEmpty()) {
            evidence = "无";
        } else {
            evidence = glossaryPrompt + "\n" + knowledgePrompt;
        }

        Map<String, Object> result = new HashMap<>();
        result.put(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, evidence);
        result.put(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, rewriteQuery);
        return result;
    }

    /** 业务术语向量召回。 */
    public List<Document> retrieveGlossaryKnowledge(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.GLOSSARY_KNOWLEDGE),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(4)
                .build();
        return vectorStore.similaritySearch(request);
    }

    /** 历史问答向量召回。 */
    public List<Document> retrieveKnowledge(String question, String databaseId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var expression = builder.and(
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.VECTOR_TYPE,
                        DataAgentSpec.Retrieval.VectorType.QUESTION_KNOWLEDGE),
                builder.eq(DataAgentSpec.Retrieval.DocumentMetadataKey.DATABASE_ID, databaseId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .filterExpression(expression)
                .topK(4)
                .build();
        return vectorStore.similaritySearch(request);
    }

    /**
     * 兼容 metadata 中 knowledgeId 既可能是 UUID 也可能是 String 的情况。
     */
    private static Optional<UUID> uuidOrNull(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return Optional.empty();
        }
        Object value = metadata.get(key);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String s && !s.isBlank()) {
            try {
                return Optional.of(UUID.fromString(s));
            } catch (IllegalArgumentException ignore) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
