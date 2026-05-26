package com.libambu.dataagent.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图表展示规格模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisplaySpec {

    /** 图表类型，如：table, bar, line, pie 等 */
    private String type;

    /** 图表标题 */
    private String title;

    /** X 轴字段名 */
    private String x;

    /** Y 轴字段名列表 */
    private List<String> y;
}
