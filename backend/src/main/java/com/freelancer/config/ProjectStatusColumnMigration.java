package com.freelancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectStatusColumnMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProjectStatusColumnMigration.class);
    private static final String COLUMN_QUERY = """
            SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'projects'
              AND COLUMN_NAME = 'status'
            """;
    private static final String ALTER_COLUMN =
            "ALTER TABLE projects MODIFY COLUMN status VARCHAR(50) NOT NULL";

    private final JdbcTemplate jdbcTemplate;

    public ProjectStatusColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ColumnDefinition> columns = jdbcTemplate.query(
                COLUMN_QUERY,
                (resultSet, rowNum) -> new ColumnDefinition(
                        resultSet.getString("DATA_TYPE"),
                        resultSet.getLong("CHARACTER_MAXIMUM_LENGTH")));

        if (columns.isEmpty()) {
            throw new IllegalStateException(
                    "The projects.status column was not found; project status migration cannot continue.");
        }

        ColumnDefinition column = columns.get(0);
        if ("varchar".equalsIgnoreCase(column.dataType()) && column.length() >= 50) {
            return;
        }

        jdbcTemplate.execute("SET SESSION lock_wait_timeout = 10");
        jdbcTemplate.execute(ALTER_COLUMN);
        logger.info("Migrated projects.status to VARCHAR(50).");
    }

    private record ColumnDefinition(String dataType, long length) {
    }
}
