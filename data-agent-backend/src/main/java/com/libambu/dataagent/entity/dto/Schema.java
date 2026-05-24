package com.libambu.dataagent.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.libambu.dataagent.agent.datasource.SchemaDataSourceProvider;
import com.libambu.dataagent.agent.datasource.SqliteSchemaDataSourceProvider;
import com.libambu.dataagent.entity.dataset.DbColumn;
import com.libambu.dataagent.entity.dataset.DbForeignKey;
import com.libambu.dataagent.entity.dataset.DbTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schema Linking 的输出模型，同时负责构造下游 prompt 中使用的 schema 文本。
 */
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class Schema {

    private static final SchemaDataSourceProvider DEFAULT_DATA_SOURCE_PROVIDER = new SqliteSchemaDataSourceProvider();

    private String databaseId;
    private List<DbTableSchemaView> dbTables = new ArrayList<>();
    private List<DbForeignKeySchemaView> dbForeignKeys = new ArrayList<>();
    private boolean enableExampleSampling = false;

    public Schema(String databaseId,
                  List<DbTableSchemaView> dbTables,
                  List<DbForeignKeySchemaView> dbForeignKeys) {
        this.databaseId = databaseId;
        this.dbTables = dbTables;
        this.dbForeignKeys = dbForeignKeys;
    }

    public String buildSchemePrompt() {
        return buildSchemePrompt(DEFAULT_DATA_SOURCE_PROVIDER);
    }

    public String buildSchemePrompt(SchemaDataSourceProvider dataSourceProvider) {
        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("【DB_ID】 ").append(databaseId).append("\n");
        for (DbTableSchemaView dbTable : safeList(dbTables)) {
            schemaBuilder.append(buildTablePrompt(dbTable, dataSourceProvider));
        }
        String keys = safeList(dbForeignKeys).stream()
                .map(DbForeignKeySchemaView::toExpression)
                .filter(Objects::nonNull)
                .filter(it -> !it.isBlank())
                .collect(Collectors.joining("\n"));
        schemaBuilder.append("【Foreign keys】\n").append(keys);
        log.info("scheme prompt {}", schemaBuilder);
        return schemaBuilder.toString();
    }

    public String buildTablePrompt(DbTableSchemaView dbTable, SchemaDataSourceProvider dataSourceProvider) {
        StringBuilder builder = new StringBuilder();
        List<DbColumnSchemaView> columns = safeList(dbTable.getColumns());
        Set<String> primaryKeys = columns.stream()
                .filter(DbColumnSchemaView::isPrimaryKey)
                .map(DbColumnSchemaView::getName)
                .collect(Collectors.toSet());
        Map<String, List<String>> examplesByColumn = enableExampleSampling
                ? loadExamples(dbTable, dataSourceProvider)
                : Collections.emptyMap();

        builder.append("# Table: ").append(dbTable.getName()).append("\n[\n");
        for (DbColumnSchemaView column : columns) {
            builder.append("(")
                    .append(column.getName())
                    .append(": ")
                    .append(column.getType())
                    .append("\n, ")
                    .append(column.getDescription())
                    .append(", ");
            if (primaryKeys.contains(column.getName())) {
                builder.append("primaryKey, ");
            }
            List<String> examples = examplesByColumn.getOrDefault(column.getName(), Collections.emptyList());
            builder.append("Examples: [")
                    .append(String.join(", ", examples))
                    .append("]),\n");
        }
        builder.append("]\n");
        return builder.toString();
    }

    private Map<String, List<String>> loadExamples(DbTableSchemaView dbTable, SchemaDataSourceProvider dataSourceProvider) {
        try (Connection connection = dataSourceProvider.get(databaseId).getConnection()) {
            return safeList(dbTable.getColumns()).stream()
                    .collect(Collectors.toMap(
                            DbColumnSchemaView::getName,
                            column -> fetchDistinctValues(connection, dbTable.getName(), column.getName(), 3),
                            (left, right) -> left
                    ));
        } catch (Exception e) {
            log.warn("load schema examples failed for table={}, databaseId={}, fallback to empty examples",
                    dbTable.getName(), databaseId, e);
            return Collections.emptyMap();
        }
    }

    public List<String> fetchDistinctValues(Connection connection,
                                            String fullTableName,
                                            String columnName,
                                            int limit) {
        List<String> values = new ArrayList<>();
        String sql = String.format(
                "SELECT DISTINCT `%s` FROM %s WHERE `%s` IS NOT NULL LIMIT %d",
                columnName, fullTableName, columnName, limit
        );
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String value = resultSet.getString(1);
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            }
        } catch (SQLException ignored) {
            // 部分列类型或表名在 SQLite 中可能不支持 DISTINCT 采样，忽略即可。
        }
        return values;
    }

    private static <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbTableSchemaView {
        private String name;
        private List<DbColumnSchemaView> columns = new ArrayList<>();

        public DbTableSchemaView(DbTable table) {
            this.name = table.getName();
            this.columns = safeList(table.getColumns()).stream()
                    .map(DbColumnSchemaView::new)
                    .toList();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbColumnSchemaView {
        private String name;
        private String type;
        private String description;
        @JsonProperty("isPrimaryKey")
        private boolean primaryKey;

        public DbColumnSchemaView(DbColumn column) {
            this.name = column.getName();
            this.type = column.getType();
            this.description = column.getDescription();
            this.primaryKey = column.isPrimaryKey();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbForeignKeySchemaView {
        private DbForeignKeyColumnView sourceColumn;
        private DbForeignKeyColumnView targetColumn;

        public DbForeignKeySchemaView(DbForeignKey foreignKey) {
            this.sourceColumn = new DbForeignKeyColumnView(foreignKey.getSourceColumn());
            this.targetColumn = new DbForeignKeyColumnView(foreignKey.getTargetColumn());
        }

        public String toExpression() {
            if (sourceColumn == null || targetColumn == null
                    || sourceColumn.getDbTable() == null || targetColumn.getDbTable() == null) {
                return "";
            }
            return sourceColumn.getDbTable().getName() + "." + sourceColumn.getName()
                    + " = "
                    + targetColumn.getDbTable().getName() + "." + targetColumn.getName();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbForeignKeyColumnView {
        private String name;
        private DbForeignKeyTableView dbTable;

        public DbForeignKeyColumnView(DbColumn column) {
            if (column == null) {
                return;
            }
            this.name = column.getName();
            this.dbTable = new DbForeignKeyTableView(column.getDbTable());
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DbForeignKeyTableView {
        private String name;

        public DbForeignKeyTableView(DbTable table) {
            if (table != null) {
                this.name = table.getName();
            }
        }
    }
}
