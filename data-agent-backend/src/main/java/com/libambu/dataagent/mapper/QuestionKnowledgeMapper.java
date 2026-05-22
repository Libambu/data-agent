package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/** 历史问答 Mapper。 */
@Mapper
public interface QuestionKnowledgeMapper {

    int insert(QuestionKnowledge entity);

    int batchInsert(@Param("list") List<QuestionKnowledge> list);

    int deleteAll();

    List<QuestionKnowledge> findByDatabaseId(@Param("databaseId") String databaseId);

    /**
     * 按主键批量查询，给知识召回节点用：先从向量库拿到 knowledgeId，再回库取 question/answer。
     */
    List<QuestionKnowledge> findByIds(@Param("ids") List<UUID> ids);
}
