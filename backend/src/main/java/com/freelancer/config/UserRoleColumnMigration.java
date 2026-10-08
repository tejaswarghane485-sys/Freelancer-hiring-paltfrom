package com.freelancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserRoleColumnMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleColumnMigration.class);
    private static final String COLUMN_QUERY = """
            SELECT COLUMN_TYPE, IS_NULLABLE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'role'
            """;
    private static final String ALTER_COLUMN =
            "ALTER TABLE users MODIFY COLUMN role ENUM('CLIENT','FREELANCER','ADMIN') NOT NULL";

    private final JdbcTemplate jdbcTemplate;

    public UserRoleColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<ColumnDefinition> columns = jdbcTemplate.query(
                    COLUMN_QUERY,
                    (resultSet, rowNum) -> new ColumnDefinition(
                            resultSet.getString("COLUMN_TYPE"),
                            resultSet.getString("IS_NULLABLE")));

            if (columns.isEmpty()) {
                logger.error("The users.role column was not found; admin role schema migration was skipped.");
                return;
            }

            ColumnDefinition column = columns.get(0);
            String columnType = column.columnType().toLowerCase(Locale.ROOT);
            if (columnType.startsWith("enum(")
                    && columnType.contains("'client'")
                    && columnType.contains("'freelancer'")
                    && columnType.contains("'admin'")
                    && "NO".equalsIgnoreCase(column.nullable())) {
                return;
            }

            jdbcTemplate.execute(ALTER_COLUMN);
            logger.info("Migrated users.role to include CLIENT, FREELANCER, and ADMIN.");
        } catch (DataAccessException exception) {
            logger.error("Could not migrate users.role; application startup will continue.", exception);
        }
    }

    private record ColumnDefinition(String columnType, String nullable) {
    }
}
