package com.libambu.dataagent.entity.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 业务库的"列"元数据：
 * <pre>
 *   (id, name, type, description, isPrimaryKey, dbTable)
 * </pre>
 * 未使用 ORM 的 ManyToOne，因此把 dbTable 拆成：
 * <ul>
 *   <li>{@link #tableId} —— 实际持久化的外键列；</li>
 *   <li>{@link #dbTable} —— 仅在查询时通过 join 填充，方便 toDocument() 拿到 databaseId / tableName。</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbColumn {
    private UUID id;
    private String name;
    private String type;
    private String description;
    private boolean primaryKey;

    /** 外键，关联 db_table.id */
    private UUID tableId;

    /**
     * 关联的 DbTable 对象（非数据库列）。
     * 仅在 findByDatabaseId 等需要 join 的查询里会被填充；
     * 普通 INSERT 不写入该字段。
     */
    private DbTable dbTable;
}
