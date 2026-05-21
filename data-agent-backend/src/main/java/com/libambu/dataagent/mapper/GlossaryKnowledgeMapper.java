package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.GlossaryKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务名词 Mapper。
 * <p>
 * batchInsertIgnore 等价于 kt 版 SaveMode.INSERT_ONLY 配合 @Key 唯一约束（这里 GlossaryKnowledge
 * 没有显式 @Key 唯一约束，依赖 id 主键即可）。
 */
@Mapper
public interface GlossaryKnowledgeMapper {

    int insert(GlossaryKnowledge entity);

    int batchInsert(@Param("list") List<GlossaryKnowledge> list);

    int deleteAll();

    List<GlossaryKnowledge> findByDatabaseId(@Param("databaseId") String databaseId);
}
