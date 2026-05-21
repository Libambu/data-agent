package com.libambu.dataagent.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

/**
 * 兼容 PostgreSQL {@code uuid} 列的 UUID TypeHandler。
 *
 * <p>MyBatis 内置的 {@code org.apache.ibatis.type.UUIDTypeHandler} 走的是 {@code setString}，
 * 在 PostgreSQL 严格类型下会报 "column ... is of type uuid but expression is of type character varying"。</p>
 *
 * <p>这里改用 {@link PreparedStatement#setObject(int, Object, int)} 配合 {@link Types#OTHER}，
 * 让 pgjdbc 把 {@link UUID} 直接当作 PG 的 uuid 类型写入。读取时用
 * {@link ResultSet#getObject(String)}，pgjdbc 会直接返回 {@link UUID}。</p>
 */
@MappedTypes(UUID.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class PostgresUuidTypeHandler extends BaseTypeHandler<UUID> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        // 用 OTHER 让 pgjdbc 把 UUID 作为 PostgreSQL uuid 列处理
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        return toUuid(value);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex);
        return toUuid(value);
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex);
        return toUuid(value);
    }

    private static UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID u) {
            return u;
        }
        // 兜底：某些场景下驱动可能返回 String
        return UUID.fromString(value.toString());
    }
}
