package com.libambu.dataagent.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python 执行结果封装。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PythonExecutionResult {

    /** 是否执行成功 */
    private boolean success;

    /** 标准输出内容 */
    private String output;

    /** 错误信息 */
    private String error;
}
