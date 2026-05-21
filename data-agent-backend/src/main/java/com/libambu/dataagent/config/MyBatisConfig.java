package com.libambu.dataagent.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.UUID;

/**
 * MyBatis 手动装配。
 *
 * <p>背景：当前工程使用 Spring Boot 4.0.x（Spring Framework 7），
 * 而 {@code mybatis-spring-boot-starter:3.0.4} 仅与 Spring Boot 3.x（Spring 6）兼容，
 * 其自动装配在 Spring Boot 4 下不会生效，导致 {@code SqlSessionFactory} 无法被创建，
 * 启动时抛出 "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required"。</p>
 *
 * <p>这里手动定义 {@link SqlSessionFactory} / {@link SqlSessionTemplate}，
 * 让 {@code @MapperScan} 扫描出来的 Mapper 代理可以正确拿到依赖。</p>
 */
@Configuration
public class MyBatisConfig {

    /** mapper XML 文件位置，默认与 application.yml 中 mybatis.mapper-locations 保持一致 */
    @Value("${mybatis.mapper-locations:classpath:mapper/**/*.xml}")
    private String mapperLocations;

    /** 实体类别名扫描包，与 application.yml 中 mybatis.type-aliases-package 对齐 */
    @Value("${mybatis.type-aliases-package:com.libambu.dataagent.entity.dataset}")
    private String typeAliasesPackage;

    /** 是否开启下划线转驼峰，与 application.yml 中 mybatis.configuration.map-underscore-to-camel-case 对齐 */
    @Value("${mybatis.configuration.map-underscore-to-camel-case:true}")
    private boolean mapUnderscoreToCamelCase;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);

        // 实体别名扫描
        if (typeAliasesPackage != null && !typeAliasesPackage.isBlank()) {
            factoryBean.setTypeAliasesPackage(typeAliasesPackage);
        }

        // 加载 mapper xml
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources(mapperLocations);
        if (resources.length > 0) {
            factoryBean.setMapperLocations(resources);
        }

        // MyBatis 全局配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);

        // 注册 PostgreSQL 友好的 UUID TypeHandler：
        //   - 写入用 setObject(Types.OTHER)，避免 "is of type uuid but expression is of type varchar"
        //   - 读取用 getObject()，pgjdbc 会直接返回 java.util.UUID
        // 同时显式注册到 (UUID, OTHER) 与 (UUID, null) 两个槽位，
        // 让 ResultMap 里写了 javaType="java.util.UUID" 但没写 jdbcType 的字段也能命中。
        PostgresUuidTypeHandler uuidTypeHandler = new PostgresUuidTypeHandler();
        configuration.getTypeHandlerRegistry().register(UUID.class, JdbcType.OTHER, uuidTypeHandler);
        configuration.getTypeHandlerRegistry().register(UUID.class, uuidTypeHandler);

        factoryBean.setConfiguration(configuration);

        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
