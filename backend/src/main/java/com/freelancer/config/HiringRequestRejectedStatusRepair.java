package com.freelancer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class HiringRequestRejectedStatusRepair implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(HiringRequestRejectedStatusRepair.class);
    private static final String MIGRATION_KEY = "repair-rejected-hiring-requests-20261008";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public HiringRequestRejectedStatusRepair(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS application_data_migrations (
                    migration_key VARCHAR(100) NOT NULL PRIMARY KEY,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            int migrationStarted = jdbcTemplate.update(
                    "INSERT IGNORE INTO application_data_migrations (migration_key) VALUES (?)",
                    MIGRATION_KEY);
            if (migrationStarted == 0) {
                return;
            }

            int repairedRequests = jdbcTemplate.update(
                    "UPDATE hiring_requests SET status = 'ACCEPTED' WHERE status = 'REJECTED'");
            logger.info("Repaired {} hiring requests previously marked REJECTED.", repairedRequests);
        });
    }
}
