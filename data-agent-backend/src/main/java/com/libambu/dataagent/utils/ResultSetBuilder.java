package com.libambu.dataagent.utils;

import com.libambu.dataagent.entity.dto.SqlResultSet;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JDBC ResultSet 到 SqlResultSet 的构建工具。
 */
public final class ResultSetBuilder {

    private ResultSetBuilder() {
    }

    /**
     * 从 JDBC ResultSet 构建 SqlResultSet，最多读取 1000 行。
     */
    public static SqlResultSet buildFrom(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnsCount = metaData.getColumnCount();

        // 获取原始列名
        List<String> rowHead = new ArrayList<>();
        for (int i = 1; i <= columnsCount; i++) {
            rowHead.add(metaData.getColumnLabel(i));
        }

        List<Map<String, String>> rawData = new ArrayList<>();
        int count = 0;

        while (rs.next() && count < 1000) {
            Map<String, String> kv = new LinkedHashMap<>();
            for (String h : rowHead) {
                String value = rs.getString(h);
                kv.put(h, value != null ? value : "");
            }
            rawData.add(kv);
            count++;
        }

        // 清理列名和数据
        List<String> cleanedHead = cleanColumnNames(rowHead);
        List<Map<String, String>> cleanedData = cleanResultSet(rawData);

        SqlResultSet result = new SqlResultSet();
        result.setColumn(cleanedHead);
        result.setData(cleanedData);
        return result;
    }

    private static List<String> cleanColumnNames(List<String> columnNames) {
        return columnNames.stream()
                .map(name -> name.replace("`", "").replace("\"", ""))
                .collect(Collectors.toList());
    }

    private static List<Map<String, String>> cleanResultSet(List<Map<String, String>> data) {
        return data.stream()
                .map(row -> {
                    Map<String, String> cleaned = new LinkedHashMap<>();
                    row.forEach((key, value) ->
                            cleaned.put(key.replace("`", "").replace("\"", ""), value));
                    return cleaned;
                })
                .collect(Collectors.toList());
    }
}
