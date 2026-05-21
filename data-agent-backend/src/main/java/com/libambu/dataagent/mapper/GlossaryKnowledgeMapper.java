package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.GlossaryKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 业务名词 Mapper。
 * <p>
 * GlossaryKnowledge 本身没有业务唯一键，依赖 id 主键即可；batchInsert 不做冲突处理。
 */
@Mapper
public interface GlossaryKnowledgeMapper {

    int insert(GlossaryKnowledge entity);

    int batchInsert(@Param("list") List<GlossaryKnowledge> list);

    int deleteAll();

    List<GlossaryKnowledge> findByDatabaseId(@Param("databaseId") String databaseId);
}
