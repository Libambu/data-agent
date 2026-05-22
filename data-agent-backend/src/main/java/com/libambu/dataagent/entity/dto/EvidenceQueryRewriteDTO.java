package com.libambu.dataagent.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识召回前对用户最新输入做"查询重写"得到的独立完整问题。
 * 用于向量库语义检索。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceQueryRewriteDTO {

    @JsonPropertyDescription("重写后的完整句子")
    @JsonProperty("standalone_query")
    private String standaloneQuery = "";
}
