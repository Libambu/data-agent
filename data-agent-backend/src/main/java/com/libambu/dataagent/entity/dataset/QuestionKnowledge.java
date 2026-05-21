package com.libambu.dataagent.entity.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 历史问答知识。对齐 kt 版 QuestionKnowledge：
 * (id, databaseId, question, answer)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionKnowledge {
    private UUID id;
    private String databaseId;
    private String question;
    private String answer;
}
