package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.DbTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * DbTable Mapper。
 * <p>
 * 模拟 kt 版 Jimmer 的 saveAll(SaveMode.INSERT_ONLY)+@Key：
 * batchInsertIgnore 在 (database_id, name) 冲突时丢弃。
 */
@Mapper
public interface DbTableMapper {

    int insert(DbTable entity);

    /** 批量插入；与 (database_id, name) 唯一约束冲突时跳过。 */
    int batchInsertIgnore(@Param("list") List<DbTable> list);

    int deleteAll();

    /** 根据 (databaseId, name) 唯一键查询，用于 saveAll 后回填 ID。 */
    DbTable findByDatabaseIdAndName(@Param("databaseId") String databaseId,
                                    @Param("name") String name);

    List<DbTable> findByDatabaseId(@Param("databaseId") String databaseId);

    DbTable findById(@Param("id") UUID id);
}
