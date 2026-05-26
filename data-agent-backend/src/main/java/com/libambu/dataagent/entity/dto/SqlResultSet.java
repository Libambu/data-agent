package com.libambu.dataagent.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL 执行结果集模型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlResultSet implements Cloneable {

    private List<String> column = new ArrayList<>();
    private List<Map<String, String>> data = new ArrayList<>();
    private String errorMsg;

    @Override
    public SqlResultSet clone() {
        return new SqlResultSet(
                new ArrayList<>(this.column),
                this.data.stream().map(HashMap::new).collect(Collectors.toList()),
                this.errorMsg
        );
    }
}
