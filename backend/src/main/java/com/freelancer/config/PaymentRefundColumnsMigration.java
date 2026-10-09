package com.freelancer.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentRefundColumnsMigration implements ApplicationRunner {

    private static final String COLUMN_QUERY = """
            SELECT COLUMN_NAME
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'payments'
              AND COLUMN_NAME IN ('requested_refund_amount', 'refunded_amount')
            """;

    private final JdbcTemplate jdbcTemplate;

    public PaymentRefundColumnsMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> existingColumns = jdbcTemplate.query(
                COLUMN_QUERY,
                (resultSet, rowNum) -> resultSet.getString("COLUMN_NAME"));

        if (!existingColumns.contains("requested_refund_amount")) {
            jdbcTemplate.execute(
                    "ALTER TABLE payments ADD COLUMN requested_refund_amount DECIMAL(12,2) NULL");
        }
        if (!existingColumns.contains("refunded_amount")) {
            jdbcTemplate.execute(
                    "ALTER TABLE payments ADD COLUMN refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00");
        }
    }
}
