package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.DbColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * DbColumn Mapper。findByDatabaseId 会同时 join 出 db_table 信息填充 dbTable 字段，
 * 供 toDocument() 拼接元数据时使用。
 */
@Mapper
public interface DbColumnMapper {

    int insert(DbColumn entity);

    int batchInsertIgnore(@Param("list") List<DbColumn> list);

    int deleteAll();

    DbColumn findByTableIdAndName(@Param("tableId") UUID tableId,
                                  @Param("name") String name);

    /**
     * 根据 databaseId 查列；同时 join 出 db_table 信息塞到 dbTable 字段里。
     * 用于 toDocument() 拼接元数据时取 databaseId / tableId。
     */
    List<DbColumn> findByDatabaseId(@Param("databaseId") String databaseId);
}
