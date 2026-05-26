package com.libambu.dataagent.agent.datasource;

import org.sqlite.SQLiteDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BIRD 数据集的 SQLite 数据源提供器。
 */
@Component
public class SqliteSchemaDataSourceProvider implements SchemaDataSourceProvider {

    private final Map<String, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    @Override
    public DataSource get(String databaseId) {
        return dataSourceCache.computeIfAbsent(databaseId, id -> {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite::resource:dev_20240627/dev_databases/" + id + "/" + id + ".sqlite");
            return dataSource;
        });
    }
}
