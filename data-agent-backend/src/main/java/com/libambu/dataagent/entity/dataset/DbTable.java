package com.libambu.dataagent.entity.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 业务库的"表"元数据：(id, name, description, databaseId)。
 * <p>
 * 实体上不挂 columns 反向集合，需要列信息时直接通过
 * DbColumnMapper.findByDatabaseId / findByTableId 查询。
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
