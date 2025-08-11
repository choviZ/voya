package com.zcw.voya.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.keygen.KeyGenerators;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * Mybatis-Flex代码生成器
 */
public class MybatisFlexGenerator {

    private final static String[] TABLE_NAMES = new String[]{
            "user",
    };

    public static void main(String[] args) {
        // 配置数据源
        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        // 配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 创建配置内容
        GlobalConfig globalConfig = createGlobalConfigUseStyle();

        // 通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        // 设置主键为雪花算法
        FlexGlobalConfig.KeyConfig keyConfig = new FlexGlobalConfig.KeyConfig();
        keyConfig.setKeyType(KeyType.Generator);
        keyConfig.setValue(KeyGenerators.snowFlakeId);
        keyConfig.setBefore(true);
        FlexGlobalConfig.getDefaultConfig().setKeyConfig(keyConfig);

        // 生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfigUseStyle() {
        // 创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        // 设置根包
        globalConfig.getPackageConfig()
                .setBasePackage("com.zcw.voya.genresult");

        // 设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setTablePrefix("tb_")
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("isDelete");
        // 设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);

        // 设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();
        // 设置生成 controller
        globalConfig.enableController();
        // 设置生成 service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();
        // 不添加日期注释
        globalConfig.getJavadocConfig()
                .setSince("");

        return globalConfig;
    }
}
