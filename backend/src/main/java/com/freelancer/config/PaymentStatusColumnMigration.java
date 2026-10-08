package com.freelancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentStatusColumnMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatusColumnMigration.class);
    private static final String COLUMN_QUERY = """
            SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, COLUMN_DEFAULT
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'payments'
              AND COLUMN_NAME = 'status'
            """;
    private static final String ALTER_COLUMN =
            "ALTER TABLE payments MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING'";

    private final JdbcTemplate jdbcTemplate;

    public PaymentStatusColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ColumnDefinition> columns = jdbcTemplate.query(
                COLUMN_QUERY,
                (resultSet, rowNum) -> new ColumnDefinition(
                        resultSet.getString("DATA_TYPE"),
                        resultSet.getLong("CHARACTER_MAXIMUM_LENGTH"),
                        resultSet.getString("COLUMN_DEFAULT")));

        if (columns.isEmpty()) {
            throw new IllegalStateException("The payments.status column was not found; payment schema migration cannot continue.");
        }

        ColumnDefinition column = columns.get(0);
        if ("varchar".equalsIgnoreCase(column.dataType())
                && column.length() >= 50
                && "PENDING".equalsIgnoreCase(column.defaultValue())) {
            return;
        }

        jdbcTemplate.execute("SET SESSION lock_wait_timeout = 10");
        jdbcTemplate.execute(ALTER_COLUMN);
        logger.info("Migrated payments.status to VARCHAR(50) with default PENDING.");
    }

    private record ColumnDefinition(String dataType, long length, String defaultValue) {
    }
}
