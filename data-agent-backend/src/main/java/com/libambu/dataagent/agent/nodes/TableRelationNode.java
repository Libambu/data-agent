package com.libambu.dataagent.agent.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.agent.prompt.PromptManager;
import com.libambu.dataagent.entity.constant.DataAgentSpec;
import com.libambu.dataagent.entity.dataset.DbColumn;
import com.libambu.dataagent.entity.dataset.DbForeignKey;
import com.libambu.dataagent.entity.dataset.DbTable;
import com.libambu.dataagent.entity.dto.Schema;
import com.libambu.dataagent.mapper.DbForeignKeyMapper;
import com.libambu.dataagent.mapper.DbTableMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Schema Linking 节点。
 *
 * <p>先合并表/列向量召回结果，再通过外键扩展关联表，最后交给模型做一次表级裁剪。</p>
 */
@Component
@Slf4j
public class TableRelationNode implements NodeAction {

    private static final double BASE_HIGH_SIMILARITY_THRESHOLD = 0.4;
    private static final int TARGET_TABLE_COUNT = 4;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private DbForeignKeyMapper dbForeignKeyMapper;

    @Autowired
    private DbTableMapper dbTableMapper;

    @Autowired
    private PromptManager promptManager;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String rewriteQuery = state.value(DataAgentSpec.Graph.StateKey.Recall.REWRITE_QUERY, "");
        String evidence = state.value(DataAgentSpec.Graph.StateKey.Recall.EVIDENCE, "");
        List<Document> tableDocuments = state.value(DataAgentSpec.Graph.StateKey.Recall.TABLE_SCHEMA, Collections.emptyList());
        List<Document> columnDocuments = state.value(DataAgentSpec.Graph.StateKey.Recall.COLUMN_SCHEMA, Collections.emptyList());
        String databaseId = state.value(DataAgentSpec.Graph.StateKey.Input.DATABASE_ID, "");

        Map<UUID, Double> tableScores = tableDocuments.stream()
                .map(document -> uuidOrNull(document.getMetadata(), DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID)
                        .flatMap(tableId -> scoreOrNull(document).map(score -> Map.entry(tableId, score))))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Math::max,
                        LinkedHashMap::new
                ));

        List<ColumnScore> columnScores = columnDocuments.stream()
                .map(this::toColumnScore)
                .flatMap(Optional::stream)
                .toList();

        ThresholdSelection thresholdSelection = selectThreshold(tableScores, columnScores);
        List<UUID> tableIds = thresholdSelection.getTableIds();
        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = thresholdSelection.getHighSimilarityColumnIdsByTable();

        List<DbForeignKey> foreignKeys = dbForeignKeyMapper.findByDatabaseId(databaseId);
        List<DbForeignKey> relatedForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> hasAnyTable(foreignKey, tableIds))
                .toList();

        Set<UUID> foreignKeyRelatedTableIds = relatedForeignKeys.stream()
                .flatMap(foreignKey -> List.of(
                        foreignKey.getSourceColumn().getDbTable().getId(),
                        foreignKey.getTargetColumn().getDbTable().getId()
                ).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        LinkedHashSet<UUID> mergedTableIds = new LinkedHashSet<>();
        mergedTableIds.addAll(tableIds);
        mergedTableIds.addAll(foreignKeyRelatedTableIds);
        List<UUID> finalTableIds = new ArrayList<>(mergedTableIds);

        Map<UUID, Set<UUID>> foreignKeyColumnIdsByTable = relatedForeignKeys.stream()
                .flatMap(foreignKey -> List.of(foreignKey.getSourceColumn(), foreignKey.getTargetColumn()).stream())
                .collect(Collectors.groupingBy(
                        column -> column.getDbTable().getId(),
                        Collectors.mapping(DbColumn::getId, Collectors.toSet())
                ));

        List<DbTable> tables = finalTableIds.isEmpty()
                ? Collections.emptyList()
                : dbTableMapper.findByIdsWithColumns(finalTableIds).stream()
                .map(table -> filterColumns(table, highSimilarityColumnIdsByTable, foreignKeyColumnIdsByTable))
                .toList();

        log.info("table relation recall merged tables: direct={}, column={}, highTable={}, highColumn={}, threshold={}, fkExpanded={}, merged={}",
                tableScores.keySet().size(),
                thresholdSelection.getColumnTableScoreKeys(),
                thresholdSelection.getHighSimilarityTableCount(),
                thresholdSelection.getHighSimilarityColumnTableCount(),
                thresholdSelection.getThreshold(),
                foreignKeyRelatedTableIds.size(),
                finalTableIds.size());

        String schemaInfo = new Schema(
                databaseId,
                tables.stream().map(Schema.DbTableSchemaView::new).toList(),
                relatedForeignKeys.stream().map(Schema.DbForeignKeySchemaView::new).toList()
        ).buildSchemePrompt();

        String prompt = promptManager.getMixSelectorPromptTemplate().render(Map.of(
                "schema_info", schemaInfo,
                "question", rewriteQuery,
                "evidence", evidence
        ));
        log.info("mix select prompt {}", prompt);

        String filterResult = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .extraBody(Map.of("enable_thinking", false)))
                .user(prompt)
                .call()
                .content();
        if (filterResult == null || filterResult.isBlank()) {
            throw new IllegalArgumentException("mix select fail");
        }

        List<String> filterTableNames = parseTableNames(filterResult);
        List<DbTable> filterTables = filterTableNames.isEmpty()
                ? Collections.emptyList()
                : dbTableMapper.findByDatabaseIdAndNames(databaseId, filterTableNames);
        List<DbForeignKey> filterForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> filterTableNames.contains(foreignKey.getTargetColumn().getDbTable().getName())
                        || filterTableNames.contains(foreignKey.getSourceColumn().getDbTable().getName()))
                .toList();

        log.info("mix select filter tables {}", filterResult);

        String tableRelation = objectMapper.writeValueAsString(new Schema(
                databaseId,
                filterTables.stream().map(Schema.DbTableSchemaView::new).toList(),
                filterForeignKeys.stream().map(Schema.DbForeignKeySchemaView::new).toList()
        ));

        return Map.of(DataAgentSpec.Graph.StateKey.Recall.TABLE_RELATION, tableRelation);
    }

    private Optional<ColumnScore> toColumnScore(Document document) {
        Optional<UUID> tableId = uuidOrNull(document.getMetadata(), DataAgentSpec.Retrieval.DocumentMetadataKey.TABLE_ID);
        Optional<UUID> columnId = uuidOrNull(document.getMetadata(), DataAgentSpec.Retrieval.DocumentMetadataKey.COLUMN_ID);
        Optional<Double> score = scoreOrNull(document);
        if (tableId.isEmpty() || columnId.isEmpty() || score.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ColumnScore(tableId.get(), columnId.get(), score.get()));
    }

    private DbTable filterColumns(DbTable table,
                                  Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable,
                                  Map<UUID, Set<UUID>> foreignKeyColumnIdsByTable) {
        Set<UUID> selectedColumnIds = new LinkedHashSet<>();
        selectedColumnIds.addAll(highSimilarityColumnIdsByTable.getOrDefault(table.getId(), Collections.emptySet()));
        selectedColumnIds.addAll(foreignKeyColumnIdsByTable.getOrDefault(table.getId(), Collections.emptySet()));
        if (selectedColumnIds.isEmpty()) {
            return table;
        }
        List<DbColumn> columns = table.getColumns() == null ? Collections.emptyList() : table.getColumns();
        table.setColumns(columns.stream()
                .filter(column -> selectedColumnIds.contains(column.getId()))
                .toList());
        return table;
    }

    private boolean hasAnyTable(DbForeignKey foreignKey, List<UUID> tableIds) {
        UUID sourceTableId = foreignKey.getSourceColumn().getDbTable().getId();
        UUID targetTableId = foreignKey.getTargetColumn().getDbTable().getId();
        return tableIds.contains(sourceTableId) || tableIds.contains(targetTableId);
    }

    private ThresholdSelection selectThreshold(Map<UUID, Double> tableScores, List<ColumnScore> columnScores) {
        ThresholdSelection best = buildSelection(BASE_HIGH_SIMILARITY_THRESHOLD, tableScores, columnScores);
        if (best.getTableIds().size() <= TARGET_TABLE_COUNT) {
            return best;
        }

        for (int i = 1; i <= 29; i++) {
            double threshold = Math.min(BASE_HIGH_SIMILARITY_THRESHOLD + i * 0.01, 0.99);
            ThresholdSelection candidate = buildSelection(threshold, tableScores, columnScores);
            int candidateDiff = Math.abs(TARGET_TABLE_COUNT - candidate.getTableIds().size());
            int bestDiff = Math.abs(TARGET_TABLE_COUNT - best.getTableIds().size());
            if (candidateDiff < bestDiff
                    || (candidateDiff == bestDiff && candidate.getThreshold() > best.getThreshold())) {
                best = candidate;
            }
        }
        return best;
    }

    private ThresholdSelection buildSelection(double threshold,
                                              Map<UUID, Double> tableScores,
                                              List<ColumnScore> columnScores) {
        Map<UUID, Double> tableScoreFromColumnRecall = columnScores.stream()
                .collect(Collectors.toMap(
                        ColumnScore::getTableId,
                        ColumnScore::getScore,
                        Math::max,
                        LinkedHashMap::new
                ));

        Set<UUID> highSimilarityTableIds = tableScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ColumnScore> highSimilarityColumnScores = columnScores.stream()
                .filter(columnScore -> columnScore.getScore() >= threshold)
                .toList();

        Set<UUID> highSimilarityColumnTableIds = highSimilarityColumnScores.stream()
                .map(ColumnScore::getTableId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = highSimilarityColumnScores.stream()
                .collect(Collectors.groupingBy(
                        ColumnScore::getTableId,
                        Collectors.mapping(ColumnScore::getColumnId, Collectors.toSet())
                ));

        LinkedHashSet<UUID> selectedTableIds = new LinkedHashSet<>();
        selectedTableIds.addAll(highSimilarityTableIds);
        selectedTableIds.addAll(highSimilarityColumnTableIds);
        if (selectedTableIds.isEmpty()) {
            topEntry(tableScores).ifPresent(selectedTableIds::add);
            topEntry(tableScoreFromColumnRecall).ifPresent(selectedTableIds::add);
        }

        return new ThresholdSelection(
                threshold,
                new ArrayList<>(selectedTableIds),
                highSimilarityColumnIdsByTable,
                highSimilarityTableIds.size(),
                highSimilarityColumnTableIds.size(),
                tableScoreFromColumnRecall.keySet().size()
        );
    }

    private Optional<UUID> topEntry(Map<UUID, Double> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private List<String> parseTableNames(String filterResult) throws Exception {
        String json = extractJsonArray(filterResult);
        return objectMapper.readValue(json, new TypeReference<List<String>>() {
        }).stream()
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    private String extractJsonArray(String text) {
        String value = text.trim();
        if (value.startsWith("```")) {
            value = value.replaceFirst("^```[a-zA-Z]*\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        int start = value.indexOf('[');
        int end = value.lastIndexOf(']');
        if (start >= 0 && end >= start) {
            return value.substring(start, end + 1);
        }
        return value;
    }

    private Optional<UUID> uuidOrNull(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return Optional.empty();
        }
        Object value = metadata.get(key);
        if (value instanceof UUID uuid) {
            return Optional.of(uuid);
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Optional.of(UUID.fromString(str));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<Double> scoreOrNull(Document document) {
        return Optional.ofNullable(document.getScore());
    }

    @Data
    @AllArgsConstructor
    private static class ColumnScore {
        private UUID tableId;
        private UUID columnId;
        private double score;
    }

    @Data
    @AllArgsConstructor
    private static class ThresholdSelection {
        private double threshold;
        private List<UUID> tableIds;
        private Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable;
        private int highSimilarityTableCount;
        private int highSimilarityColumnTableCount;
        private int columnTableScoreKeys;
    }
}
