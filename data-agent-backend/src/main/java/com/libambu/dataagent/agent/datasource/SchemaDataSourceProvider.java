package com.libambu.dataagent.agent.datasource;

import javax.sql.DataSource;

/**
 * 根据 databaseId 获取业务库数据源，用于 Schema prompt 采样示例值。
 */
@FunctionalInterface
public interface SchemaDataSourceProvider {

    DataSource get(String databaseId);
}
