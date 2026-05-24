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
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("deepseekClient")
    private ChatClient deepseekClient;

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

        //它从每个 table document 的 metadata 里取 tableId，从 document.getScore() 取相似度分数。如果同一张表出现多次，取最大分数。
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
        //它从每个 column document 的 metadata 里取 columnId，从 document.getScore() 取相似度分数。如果同一列出现多次，取最大分数。
        List<ColumnScore> columnScores = columnDocuments.stream()
                .map(this::toColumnScore)
                .flatMap(Optional::stream)
                .toList();

        //召回表
        ThresholdSelection thresholdSelection = selectThreshold(tableScores, columnScores);
        List<UUID> tableIds = thresholdSelection.getTableIds();
        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = thresholdSelection.getHighSimilarityColumnIdsByTable();

        //只靠语义召回容易漏掉“连接表”或“桥表”。所以代码会加载当前数据库的所有外键：
        List<DbForeignKey> foreignKeys = dbForeignKeyMapper.findByDatabaseId(databaseId);
        //foreignKey -> sourceTable in tableIds || targetTable in tableIds
        List<DbForeignKey> relatedForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> hasAnyTable(foreignKey, tableIds))
                .toList();

        //把所有相关外键的两端表ID 都取出来。
        Set<UUID> foreignKeyRelatedTableIds = relatedForeignKeys.stream()
                .flatMap(foreignKey -> List.of(
                        foreignKey.getSourceColumn().getDbTable().getId(),
                        foreignKey.getTargetColumn().getDbTable().getId()
                ).stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        //合并“语义召回表”和“外键扩展表”
        LinkedHashSet<UUID> mergedTableIds = new LinkedHashSet<>();
        mergedTableIds.addAll(tableIds);
        mergedTableIds.addAll(foreignKeyRelatedTableIds);
        List<UUID> finalTableIds = new ArrayList<>(mergedTableIds);
        //从相关外键里提取外键列，并按表分组
        Map<UUID, Set<UUID>> foreignKeyColumnIdsByTable = relatedForeignKeys.stream()
                .flatMap(foreignKey -> List.of(foreignKey.getSourceColumn(), foreignKey.getTargetColumn()).stream())
                .collect(Collectors.groupingBy(
                        column -> column.getDbTable().getId(),
                        Collectors.mapping(DbColumn::getId, Collectors.toSet())
                ));
        //这个 mapper 会根据表 ID 查询表，并且把列也一起查出来。结果是：这一步从数据库拿到了最终候选表的 schema
        List<DbTable> tables = finalTableIds.isEmpty()
                ? Collections.emptyList()
                : dbTableMapper.findByIdsWithColumns(finalTableIds).stream()
                .map(table -> filterColumns(table, highSimilarityColumnIdsByTable, foreignKeyColumnIdsByTable))
                .toList();

        log.info("table relation recall merged tables: direct={}, column={}, highTable={}, highColumn={}, threshold={}, fkExpanded={}, merged={}",
                tableScores.keySet().size(),//表级向量召回命中过的不同表数量
                thresholdSelection.getColumnTableScoreKeys(),//列级向量召回涉及到的不同表数量。
                thresholdSelection.getHighSimilarityTableCount(),//表召回结果中，分数超过当前阈值的表数量。
                thresholdSelection.getHighSimilarityColumnTableCount(),//列召回结果中，分数超过当前阈值后，这些列所属的表数量。
                thresholdSelection.getThreshold(),
                foreignKeyRelatedTableIds.size(),//相关外键的两端表ID 都取出来。
                finalTableIds.size());//召回表结果和外键扩展表结果数量。

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

        //调用大模型做最终表筛选,在这些候选表中，哪些表真正和用户问题相关？输出["orders", "users"]
        String filterResult = deepseekClient
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
        //根据最终表名筛选外键
        List<DbForeignKey> filterForeignKeys = foreignKeys.stream()
                .filter(foreignKey -> filterTableNames.contains(foreignKey.getTargetColumn().getDbTable().getName())
                        || filterTableNames.contains(foreignKey.getSourceColumn().getDbTable().getName()))
                .toList();

        log.info("mix select filter tables {}", filterResult);

        /**
         *   最终结构大概是：
         *
         *   {
         *     "databaseId": "xxx",
         *     "dbTables": [
         *       {
         *         "name": "orders",
         *         "columns": [
         *           {
         *             "name": "id",
         *             "type": "TEXT",
         *             "description": "订单ID",
         *             "isPrimaryKey": true
         *           },
         *           {
         *             "name": "user_id",
         *             "type": "TEXT",
         *             "description": "用户ID",
         *             "isPrimaryKey": false
         *           }
         *         ]
         *       }
         *     ],
         *     "dbForeignKeys": [
         *       {
         *         "sourceColumn": {
         *           "name": "user_id",
         *           "dbTable": {
         *             "name": "orders"
         *           }
         *         },
         *         "targetColumn": {
         *           "name": "id",
         *           "dbTable": {
         *             "name": "users"
         *           }
         *         }
         *       }
         *     ],
         *     "enableExampleSampling": false
         *   }
         */
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

    //自动选择一个合适的相似度阈值，让最终选出来的候选表数量尽量接近 TARGET_TABLE_COUNT = 4。
    //这个函数的核心目的就是：控制候选 schema 的规模.它不希望召回表太多，因为后面会把这些表结构交给大模型做二次筛选，表太多会增加 token 成本和干扰
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
        // 如果同一张表有多个字段被召回，就取字段分数最高的那个作为这张表的字段召回分数。
        Map<UUID, Double> tableScoreFromColumnRecall = columnScores.stream()
                .collect(Collectors.toMap(
                        ColumnScore::getTableId,
                        ColumnScore::getScore,
                        Math::max,
                        LinkedHashMap::new
                ));

        //从表级召回结果中，找出分数大于等于阈值的表。
        Set<UUID> highSimilarityTableIds = tableScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        //从字段级召回结果中，找出分数大于等于阈值的字段。
        List<ColumnScore> highSimilarityColumnScores = columnScores.stream()
                .filter(columnScore -> columnScore.getScore() >= threshold)
                .toList();

        //找出这些高相似字段所属的表
        //只要某张表里有一个字段高度相关，这张表也会被选入候选表。
        Set<UUID> highSimilarityColumnTableIds = highSimilarityColumnScores.stream()
                .map(ColumnScore::getTableId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        //tableId -> 这张表中高相似的 columnId 集合
        Map<UUID, Set<UUID>> highSimilarityColumnIdsByTable = highSimilarityColumnScores.stream()
                .collect(Collectors.groupingBy(
                        ColumnScore::getTableId,
                        Collectors.mapping(ColumnScore::getColumnId, Collectors.toSet())
                ));

        //合并表级命中的表和字段级命中的表,LinkedHashSet去重复
        LinkedHashSet<UUID> selectedTableIds = new LinkedHashSet<>();
        selectedTableIds.addAll(highSimilarityTableIds);
        selectedTableIds.addAll(highSimilarityColumnTableIds);
        //如果当前阈值太高，导致没有表分数达到阈值,没有字段分数达到阈值,那就至少选两个“最高分来源”
        if (selectedTableIds.isEmpty()) {
            topEntry(tableScores).ifPresent(selectedTableIds::add);
            topEntry(tableScoreFromColumnRecall).ifPresent(selectedTableIds::add);
        }

        return new ThresholdSelection(
                threshold,//本次使用的阈值
                new ArrayList<>(selectedTableIds),//选中的表ID
                highSimilarityColumnIdsByTable,//tableId -> 这张表中高相似的 columnId 集合
                highSimilarityTableIds.size(),//表级别召回中选中的表数量
                highSimilarityColumnTableIds.size(),//表示字段级召回中，达到阈值的字段所覆盖的表数量。
                tableScoreFromColumnRecall.keySet().size()//字段召回结果覆盖了多少张表。不管这些字段有没有达到阈值
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
