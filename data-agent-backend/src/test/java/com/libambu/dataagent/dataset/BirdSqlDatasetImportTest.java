package com.libambu.dataagent.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libambu.dataagent.entity.dataset.DbColumn;
import com.libambu.dataagent.entity.dataset.DbForeignKey;
import com.libambu.dataagent.entity.dataset.DbTable;
import com.libambu.dataagent.entity.dataset.GlossaryKnowledge;
import com.libambu.dataagent.entity.dataset.QuestionKnowledge;
import com.libambu.dataagent.mapper.DbColumnMapper;
import com.libambu.dataagent.mapper.DbForeignKeyMapper;
import com.libambu.dataagent.mapper.DbTableMapper;
import com.libambu.dataagent.mapper.GlossaryKnowledgeMapper;
import com.libambu.dataagent.mapper.QuestionKnowledgeMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BIRD-SQL dataset 导入测试，对齐 kt 版 BirdSqlDatasetImportTest。
 * <p>
 * 1. {@link #createKnowledgeTest()}：从 dev.json 提取 evidence 与 question/SQL，
 *    写入 glossary_knowledge / question_knowledge。
 * 2. {@link #createSchemeTest()}：从 dev_tables.json 提取库表结构，按
 *    table -> column -> foreign_key 三步批量写入。
 * <p>
 * Java 版用 MyBatis 实现，与 kt 版 Jimmer 的 SaveMode.INSERT_ONLY + @Key 等价：
 * mapper 中所有 batchInsertIgnore 都用 ON CONFLICT DO NOTHING 处理唯一键冲突。
 */
@Slf4j
@SpringBootTest
class BirdSqlDatasetImportTest {

    @Autowired
    private GlossaryKnowledgeMapper glossaryKnowledgeMapper;
    @Autowired
    private QuestionKnowledgeMapper questionKnowledgeMapper;
    @Autowired
    private DbTableMapper dbTableMapper;
    @Autowired
    private DbColumnMapper dbColumnMapper;
    @Autowired
    private DbForeignKeyMapper dbForeignKeyMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void createKnowledgeTest() throws IOException {
        List<BirdQuestion> questions = OBJECT_MAPPER.readValue(
                new ClassPathResource("dev_20240627/dev.json").getInputStream(),
                new TypeReference<List<BirdQuestion>>() {
                });
        log.info("总共读取了 {} 个问题", questions.size());

        glossaryKnowledgeMapper.deleteAll();
        questionKnowledgeMapper.deleteAll();

        // 1) 抽 evidence 入 glossary_knowledge：按 evidence 去重，仅保留非空
        Set<String> seenEvidence = new HashSet<>();
        List<GlossaryKnowledge> glossaries = new ArrayList<>();
        for (BirdQuestion q : questions) {
            if (q.getEvidence() == null || q.getEvidence().isEmpty()) {
                continue;
            }
            if (!seenEvidence.add(q.getEvidence())) {
                continue;
            }
            glossaries.add(GlossaryKnowledge.builder()
                    .id(UUID.randomUUID())
                    .databaseId(q.getDbId())
                    .term("")
                    .description(q.getEvidence())
                    .build());
        }
        if (!glossaries.isEmpty()) {
            glossaryKnowledgeMapper.batchInsert(glossaries);
        }
        log.info("写入 glossary_knowledge {} 条", glossaries.size());

        // 2) 抽 question/SQL 入 question_knowledge
        List<QuestionKnowledge> questionKnowledgeList = questions.stream()
                .map(q -> QuestionKnowledge.builder()
                        .id(UUID.randomUUID())
                        .databaseId(q.getDbId())
                        .question(q.getQuestion())
                        .answer(q.getSql())
                        .build())
                .collect(Collectors.toList());
        if (!questionKnowledgeList.isEmpty()) {
            questionKnowledgeMapper.batchInsert(questionKnowledgeList);
        }
        log.info("写入 question_knowledge {} 条", questionKnowledgeList.size());
    }

    @Test
    void createSchemeTest() throws IOException {
        List<BirdTable> birdTables = OBJECT_MAPPER.readValue(
                new ClassPathResource("dev_20240627/dev_tables.json").getInputStream(),
                new TypeReference<List<BirdTable>>() {
                });

        // -------- 准备数据 --------

        List<DbTable> tablesToSave = new ArrayList<>();
        // 索引到 columnBuilders 与 tablesToSave 同步：第 i 张 table 对应 columnBuilders.get(i)
        List<Function<DbTable, List<DbColumn>>> columnBuilders = new ArrayList<>();
        List<PendingForeignKey> pendingForeignKeys = new ArrayList<>();

        for (BirdTable databaseTable : birdTables) {
            List<String> tableNamesOriginal = databaseTable.getTableNamesOriginal();
            List<String> tableNames = databaseTable.getTableNames();

            for (int index = 0; index < tableNamesOriginal.size(); index++) {
                final int tableIndex = index;
                String tableName = tableNamesOriginal.get(index);
                String tableDescription = tableIndex < tableNames.size()
                        ? tableNames.get(tableIndex) : tableName;

                // 1) 构造 DbTable，但先不持久化
                tablesToSave.add(DbTable.builder()
                        .id(UUID.randomUUID())
                        .name(tableName)
                        .description(tableDescription)
                        .databaseId(databaseTable.getDbId())
                        .build());

                // 2) 把列的构造逻辑挂起，等表持久化拿到 ID 后再执行
                columnBuilders.add(savedTable -> {
                    List<DbColumn> result = new ArrayList<>();
                    List<List<Object>> columnNamesOriginal = databaseTable.getColumnNamesOriginal();
                    List<List<Object>> columnNames = databaseTable.getColumnNames();
                    List<String> columnTypes = databaseTable.getColumnTypes();

                    for (int columnIndex = 0; columnIndex < columnNamesOriginal.size(); columnIndex++) {
                        List<Object> info = columnNamesOriginal.get(columnIndex);
                        if (info == null || info.size() < 2) {
                            continue;
                        }
                        Object ownerTableIdx = info.get(0);
                        Object colNameObj = info.get(1);
                        if (!(ownerTableIdx instanceof Number)
                                || ((Number) ownerTableIdx).intValue() != tableIndex) {
                            continue;
                        }
                        if (!(colNameObj instanceof String) || ((String) colNameObj).isEmpty()) {
                            continue;
                        }

                        String description;
                        if (columnIndex < columnNames.size()
                                && columnNames.get(columnIndex) != null
                                && columnNames.get(columnIndex).size() >= 2
                                && columnNames.get(columnIndex).get(1) instanceof String) {
                            description = (String) columnNames.get(columnIndex).get(1);
                        } else {
                            description = (String) colNameObj;
                        }
                        String type = columnIndex < columnTypes.size() ? columnTypes.get(columnIndex) : "";

                        result.add(DbColumn.builder()
                                .id(UUID.randomUUID())
                                .name((String) colNameObj)
                                .description(description)
                                .type(type)
                                .primaryKey(isPrimaryKey(databaseTable, columnIndex))
                                .tableId(savedTable.getId())
                                .build());
                    }
                    return result;
                });
            }

            // 3) 提取外键，先用 (table, column) 名字暂存，等列持久化后再换成 column id
            List<List<Integer>> foreignKeys = databaseTable.getForeignKeys();
            if (foreignKeys == null) {
                continue;
            }
            for (List<Integer> pair : foreignKeys) {
                if (pair == null || pair.size() < 2) {
                    log.warn("跳过格式错误外键: dbId={}, pair={}", databaseTable.getDbId(), pair);
                    continue;
                }
                List<List<Object>> columnNamesOriginal = databaseTable.getColumnNamesOriginal();
                Integer srcIdx = pair.get(0);
                Integer tgtIdx = pair.get(1);
                if (srcIdx == null || tgtIdx == null
                        || srcIdx < 0 || srcIdx >= columnNamesOriginal.size()
                        || tgtIdx < 0 || tgtIdx >= columnNamesOriginal.size()) {
                    log.warn("跳过无效外键: dbId={}, pair={}", databaseTable.getDbId(), pair);
                    continue;
                }
                List<Object> sourceColumnInfo = columnNamesOriginal.get(srcIdx);
                List<Object> targetColumnInfo = columnNamesOriginal.get(tgtIdx);
                if (sourceColumnInfo == null || targetColumnInfo == null
                        || !(sourceColumnInfo.get(0) instanceof Number)
                        || !(targetColumnInfo.get(0) instanceof Number)) {
                    log.warn("跳过无效外键(列信息): dbId={}, pair={}", databaseTable.getDbId(), pair);
                    continue;
                }
                int sourceTableIndex = ((Number) sourceColumnInfo.get(0)).intValue();
                int targetTableIndex = ((Number) targetColumnInfo.get(0)).intValue();
                List<String> tnOriginal = databaseTable.getTableNamesOriginal();
                if (sourceTableIndex < 0 || sourceTableIndex >= tnOriginal.size()
                        || targetTableIndex < 0 || targetTableIndex >= tnOriginal.size()) {
                    log.warn("跳过无效外键(表索引): dbId={}, pair={}", databaseTable.getDbId(), pair);
                    continue;
                }
                pendingForeignKeys.add(new PendingForeignKey(
                        databaseTable.getDbId(),
                        tnOriginal.get(sourceTableIndex),
                        (String) sourceColumnInfo.get(1),
                        tnOriginal.get(targetTableIndex),
                        (String) targetColumnInfo.get(1)
                ));
            }
        }

        log.info("准备保存 {} 张表", tablesToSave.size());
        // 步骤 A：批量插入表（冲突跳过），然后回查带 id 的真实记录
        if (!tablesToSave.isEmpty()) {
            dbTableMapper.batchInsertIgnore(tablesToSave);
        }
        // 因为存在 ON CONFLICT DO NOTHING，本地 list 里的 id 不一定真的写进去了，
        // 必须按 (databaseId, name) 把数据库里现存的真实 ID 拉回来。
        List<DbTable> savedTables = new ArrayList<>(tablesToSave.size());
        for (DbTable t : tablesToSave) {
            DbTable real = dbTableMapper.findByDatabaseIdAndName(t.getDatabaseId(), t.getName());
            savedTables.add(Objects.requireNonNullElse(real, t));
        }

        // 步骤 B：基于 savedTables（带真实 id）生成所有列
        List<DbColumn> columnsToSave = new ArrayList<>();
        for (int i = 0; i < savedTables.size(); i++) {
            columnsToSave.addAll(columnBuilders.get(i).apply(savedTables.get(i)));
        }
        log.info("准备保存 {} 个列", columnsToSave.size());
        if (!columnsToSave.isEmpty()) {
            dbColumnMapper.batchInsertIgnore(columnsToSave);
        }
        // 用 tableId -> DbTable 的索引避免 N×M 反查 owner（kt 版直接 column.dbTable 拿到）
        Map<UUID, DbTable> tableById = new HashMap<>(savedTables.size() * 2);
        for (DbTable st : savedTables) {
            tableById.put(st.getId(), st);
        }
        // columnMap key: databaseId | tableName | columnName  -> DbColumn(已带数据库真实 id)
        Map<String, DbColumn> columnMap = new LinkedHashMap<>();
        for (DbColumn c : columnsToSave) {
            DbColumn real = dbColumnMapper.findByTableIdAndName(c.getTableId(), c.getName());
            DbColumn effective = real != null ? real : c;
            DbTable owner = tableById.get(effective.getTableId());
            if (owner == null) {
                continue;
            }
            String key = owner.getDatabaseId() + "|" + owner.getName() + "|" + effective.getName();
            columnMap.put(key, effective);
        }

        // 步骤 C：用 columnMap 把 pending 外键转成真正的 DbForeignKey
        List<DbForeignKey> foreignKeys = new ArrayList<>();
        for (PendingForeignKey p : pendingForeignKeys) {
            DbColumn src = columnMap.get(p.databaseId + "|" + p.sourceTableName + "|" + p.sourceColumnName);
            DbColumn tgt = columnMap.get(p.databaseId + "|" + p.targetTableName + "|" + p.targetColumnName);
            if (src == null || tgt == null) {
                log.warn("跳过无法匹配已保存列的外键: {}", p);
                continue;
            }
            foreignKeys.add(DbForeignKey.builder()
                    .id(UUID.randomUUID())
                    .sourceColumnId(src.getId())
                    .targetColumnId(tgt.getId())
                    .build());
        }
        log.info("读取了{}个外键", foreignKeys.size());
        if (!foreignKeys.isEmpty()) {
            dbForeignKeyMapper.batchInsertIgnore(foreignKeys);
        }
    }

    /**
     * BIRD 表的 primary_keys 元素可能是 Integer，也可能是 List&lt;Integer&gt;（联合主键）。
     * 这里把它压平后判断 columnIndex 是否在其中。
     */
    static boolean isPrimaryKey(BirdTable birdTable, int columnIndex) {
        List<Object> primaryKeys = birdTable.getPrimaryKeys();
        if (primaryKeys == null) {
            return false;
        }
        for (Object pk : primaryKeys) {
            if (pk instanceof Number) {
                if (((Number) pk).intValue() == columnIndex) {
                    return true;
                }
            } else if (pk instanceof List<?>) {
                for (Object inner : (List<?>) pk) {
                    if (inner instanceof Number && ((Number) inner).intValue() == columnIndex) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class PendingForeignKey {
        private String databaseId;
        private String sourceTableName;
        private String sourceColumnName;
        private String targetTableName;
        private String targetColumnName;
    }

    /** dev_tables.json 中每个数据库一项。 */
    @Data
    @NoArgsConstructor
    static class BirdTable {
        @JsonProperty("db_id")
        private String dbId;

        @JsonProperty("table_names_original")
        private List<String> tableNamesOriginal;

        @JsonProperty("table_names")
        private List<String> tableNames;

        @JsonProperty("column_names_original")
        private List<List<Object>> columnNamesOriginal;

        @JsonProperty("column_names")
        private List<List<Object>> columnNames;

        @JsonProperty("column_types")
        private List<String> columnTypes;

        /** 元素可能是 Integer 或 List&lt;Integer&gt;，所以泛型用 Object。 */
        @JsonProperty("primary_keys")
        private List<Object> primaryKeys;

        @JsonProperty("foreign_keys")
        private List<List<Integer>> foreignKeys;
    }

    /** dev.json 中每条问题。 */
    @Data
    @NoArgsConstructor
    static class BirdQuestion {
        @JsonProperty("question_id")
        private Integer questionId;

        @JsonProperty("db_id")
        private String dbId;

        @JsonProperty("question")
        private String question;

        @JsonProperty("evidence")
        private String evidence;

        @JsonProperty("SQL")
        private String sql;

        @JsonProperty("difficulty")
        private String difficulty;
    }
}
