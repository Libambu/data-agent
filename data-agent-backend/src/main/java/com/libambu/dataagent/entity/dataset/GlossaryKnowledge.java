package com.libambu.dataagent.entity.dataset;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

/**
 * 业务名词知识。对齐 kt 版 GlossaryKnowledge：
 * (id, databaseId, term, description, synonyms?)
 * synonyms 为可空字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlossaryKnowledge {
    private UUID id;
    private String databaseId;
    private String term;
    private String description;
    /** 同义词，可空 */
    private String synonyms;
}
