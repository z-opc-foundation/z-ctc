package com.zifang.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据库DTO
 * （2026-10-30 全仓去 Lombok：原 @Data 的合成成员在此显式展开）
 */
public class DatabaseDTO {
    private String databaseName;
    private List<TableDTO> tables = new ArrayList<>();

    public DatabaseDTO(String databaseName) {
        this.databaseName = Objects.requireNonNull(databaseName, "数据库名不能为空").trim();
    }

    public void addTable(TableDTO table) {
        if (table != null && !table.getColumns().isEmpty()) {
            tables.add(table);
        }
    }

    public List<TableDTO> getTables() {
        return new ArrayList<>(tables);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public void setTables(List<TableDTO> tables) {
        this.tables = tables == null ? new ArrayList<>() : new ArrayList<>(tables);
    }

    @Override
    public String toString() {
        return "DatabaseDTO(databaseName=" + getDatabaseName() + ", tables=" + getTables() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DatabaseDTO)) return false;
        DatabaseDTO other = (DatabaseDTO) o;
        return Objects.equals(getDatabaseName(), other.getDatabaseName())
                && Objects.equals(getTables(), other.getTables());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDatabaseName(), getTables());
    }
}
