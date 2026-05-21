package com.libambu.dataagent.mapper;

import com.libambu.dataagent.entity.dataset.QuestionKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 历史问答 Mapper。 */
@Mapper
public interface QuestionKnowledgeMapper {

    int insert(QuestionKnowledge entity);

    int batchInsert(@Param("list") List<QuestionKnowledge> list);

    int deleteAll();

    List<QuestionKnowledge> findByDatabaseId(@Param("databaseId") String databaseId);
}
