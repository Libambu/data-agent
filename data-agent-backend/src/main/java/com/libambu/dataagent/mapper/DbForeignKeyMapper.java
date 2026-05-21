package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.DbForeignKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** DbForeignKey Mapper。 */
@Mapper
public interface DbForeignKeyMapper {

    int insert(DbForeignKey entity);

    int batchInsertIgnore(@Param("list") List<DbForeignKey> list);

    int deleteAll();

    /**
     * 按 databaseId 查询；返回结果中 sourceColumn / targetColumn 都填充了 dbTable.name，
     * 供 toExpression() 使用。
     */
    List<DbForeignKey> findByDatabaseId(@Param("databaseId") String databaseId);
}
