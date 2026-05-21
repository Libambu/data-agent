package com.libambu.dataagent.entity.dataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 外键关系实体，对齐 kt 版 DbForeignKey：
 * <pre>
 *   (id, sourceColumn, targetColumn)
 * </pre>
 * Java 版同样把 ManyToOne 拆成显式 ID + 可选的内嵌对象（用于 join 后展示）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbForeignKey {
    private UUID id;

    /** 源列 id（db_column.id） */
    private UUID sourceColumnId;
    /** 目标列 id（db_column.id） */
    private UUID targetColumnId;

    /** 仅在查询时填充，方便构造 "source.col = target.col" 这种表达式。 */
    private DbColumn sourceColumn;
    /** 仅在查询时填充。 */
    private DbColumn targetColumn;

    /** 等价于 kt 版 DbForeignKeySchemaView.toExpression()。 */
    public String toExpression() {
        if (sourceColumn == null || targetColumn == null
                || sourceColumn.getDbTable() == null || targetColumn.getDbTable() == null) {
            return "";
        }
        return sourceColumn.getDbTable().getName() + "." + sourceColumn.getName()
                + " = "
                + targetColumn.getDbTable().getName() + "." + targetColumn.getName();
    }
}
