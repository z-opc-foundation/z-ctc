package com.zifang.generator;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisPlusGenerator {
    // 数据库配置
    private static final String DB_URL = "jdbc:mysql://101.37.80.51:3306/biz_service?serverTimezone=UTC&useUnicode=true&characterEncoding=utf-8";
    private static final String DB_USER = "zifang";
    private static final String DB_PASS = System.getenv("Z_BASE_DB_DEFAULT_PASSWORD") == null
            ? "" : System.getenv("Z_BASE_DB_DEFAULT_PASSWORD"); // 明文只住 lead/004_重要秘钥
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";

    // 生成配置
    private static final String BASE_PACKAGE = "com.zifang.ctc.core.domain";
    private static final String OUTPUT_DIR = "ctc-core/src/main/java";
    private static final String TEMPLATE_DIR = "templates";

    private static HikariDataSource dataSource;
    private static Configuration freemarkerConfig;

    public static void main(String[] args) {
        try {
            System.out.println("=== 开始代码生成 ===");
            initDataSource();
            initFreemarker();

            DatabaseDTO dbDTO = getDatabaseMetaData();
            if (dbDTO.getTables().isEmpty()) {
                System.out.println("未获取到表数据，生成终止");
                return;
            }

            generateAllFiles(dbDTO);
            System.out.println("=== 生成完成！输出目录：" + new File(OUTPUT_DIR).getAbsolutePath() + " ===");

        } catch (Exception e) {
            System.err.println("生成失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (dataSource != null) {
                dataSource.close();
                System.out.println("连接池已关闭");
            }
        }
    }

    private static void initDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(DB_URL);
            config.setUsername(DB_USER);
            config.setPassword(DB_PASS);
            config.setDriverClassName(DB_DRIVER);
            config.setMaximumPoolSize(3);
            config.setConnectionTimeout(5000);
            dataSource = new HikariDataSource(config);
            System.out.println("连接池初始化成功");
        } catch (Exception e) {
            throw new RuntimeException("连接池初始化失败，请检查数据库配置", e);
        }
    }

    private static void initFreemarker() throws IOException {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerConfig.setClassLoaderForTemplateLoading(
                MyBatisPlusGenerator.class.getClassLoader(),
                TEMPLATE_DIR
        );
        freemarkerConfig.setDefaultEncoding("UTF-8");
        System.out.println("Freemarker初始化成功");
    }

    private static DatabaseDTO getDatabaseMetaData() throws SQLException {
        String dbName = extractDbName(DB_URL);
        DatabaseDTO dbDTO = new DatabaseDTO(dbName);

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            System.out.println("已连接数据库：" + dbName);

            try (ResultSet tableRs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tableRs.next()) {
                    String tableName = tableRs.getString("TABLE_NAME");
                    String comment = tableRs.getString("REMARKS");

                    TableDTO tableDTO = new TableDTO(tableName, comment);
                    loadColumns(metaData, tableDTO);
                    tableDTO.debugPrint();
                    dbDTO.addTable(tableDTO);
                }
            }
        }
        return dbDTO;
    }

    private static void loadColumns(DatabaseMetaData metaData, TableDTO tableDTO) throws SQLException {
        try (ResultSet columnRs = metaData.getColumns(null, null, tableDTO.tableName, "%")) {
            while (columnRs.next()) {
                String columnName = columnRs.getString("COLUMN_NAME");
                String dataType = columnRs.getString("TYPE_NAME");
                int columnSize = columnRs.getInt("COLUMN_SIZE");
                boolean nullable = "YES".equals(columnRs.getString("IS_NULLABLE"));
                // 修复：明确传递isPrimaryKey参数
                boolean isPrimaryKey = isPrimaryKey(metaData, tableDTO.tableName, columnName);
                String comment = columnRs.getString("REMARKS");

                ColumnDTO column = new ColumnDTO(
                        columnName, dataType, columnSize, nullable, isPrimaryKey, comment
                );
                tableDTO.addColumn(column);
            }
        }
    }

    private static boolean isPrimaryKey(DatabaseMetaData metaData, String tableName, String columnName) throws SQLException {
        try (ResultSet pkRs = metaData.getPrimaryKeys(null, null, tableName)) {
            while (pkRs.next()) {
                if (columnName.equals(pkRs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractDbName(String url) {
        int start = url.lastIndexOf("/") + 1;
        int end = url.indexOf("?", start);
        if (end == -1) { end = url.length(); }

        return url.substring(start, end).trim();
    }

    private static void generateAllFiles(DatabaseDTO dbDTO) throws Exception {
        for (TableDTO table : dbDTO.getTables()) {
            generateEntity(table);
            generateMapper(table);
            generateMapperXml(table);
            generateService(table);
            generateServiceImpl(table);
        }
    }

    private static Map<String, Object> buildModel(TableDTO table) {
        Map<String, Object> model = new HashMap<>();
        model.put("basePackage", BASE_PACKAGE);
        model.put("tableName", table.tableName);
        model.put("entityName", table.entityName);
        model.put("columns", table.getColumns());
        model.put("primaryKey", table.primaryKey);
        model.put("table", table);
        return model;
    }

    private static void generateEntity(TableDTO table) throws Exception {
        Map<String, Object> model = buildModel(table);
        String pkg = BASE_PACKAGE + ".entity";
        String fileName = table.entityName + ".java";
        generateFile("entity.ftl", model, pkg, fileName);
    }

    private static void generateMapper(TableDTO table) throws Exception {
        Map<String, Object> model = buildModel(table);
        String pkg = BASE_PACKAGE + ".mapper";
        String fileName = table.entityName + "Mapper.java";
        generateFile("mapper.ftl", model, pkg, fileName);
    }

    private static void generateMapperXml(TableDTO table) throws Exception {
        Map<String, Object> model = buildModel(table);
        String pkg = BASE_PACKAGE + ".mapper.xml";
        String fileName = table.entityName + "Mapper.xml";
        generateFile("mapperXml.ftl", model, pkg, fileName);
    }

    private static void generateService(TableDTO table) throws Exception {
        Map<String, Object> model = buildModel(table);
        String pkg = BASE_PACKAGE + ".service";
        String fileName = "I" + table.entityName + "Service.java";
        generateFile("service.ftl", model, pkg, fileName);
    }

    private static void generateServiceImpl(TableDTO table) throws Exception {
        Map<String, Object> model = buildModel(table);
        String pkg = BASE_PACKAGE + ".service.impl";
        String fileName = table.entityName + "ServiceImpl.java";
        generateFile("serviceImpl.ftl", model, pkg, fileName);
    }

    private static void generateFile(String templateName, Map<String, Object> model, String pkg, String fileName) throws Exception {
        String dirPath = OUTPUT_DIR + "/" + pkg.replace(".", "/");
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("无法创建目录：" + dirPath);
        }

        File file = new File(dir, fileName);
        try (Writer out = new BufferedWriter(new FileWriter(file))) {
            Template template = freemarkerConfig.getTemplate(templateName);
            template.process(model, out);
            System.out.println("生成文件：" + file.getAbsolutePath());
        }
    }
}