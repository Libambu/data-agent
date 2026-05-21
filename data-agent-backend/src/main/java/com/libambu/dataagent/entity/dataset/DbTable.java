package com.libambu.dataagent.entity.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 业务库的"表"元数据。对齐 kt 版 DbTable：
 * (id, name, description, databaseId)
 * <p>
 * kt 版还有 columns 这个 OneToMany 反向集合，Java 版我们不在实体里挂集合，
 * 需要的时候直接通过 DbColumnMapper.findByDatabaseId / findByTableId 查询。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbTable {
    private UUID id;
    private String name;
    private String description;
    private String databaseId;
}
