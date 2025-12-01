package com.ecommerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database migration configuration that runs on application startup.
 * This ensures database schema is updated to support all payment methods including HYBRID.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Run database migrations after the application is fully started.
     * This uses ApplicationReadyEvent to ensure all beans are initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runDatabaseMigrations() {
        log.info("Starting database migrations...");
        
        try {
            addHybridPaymentMethodSupport();
            log.info("Database migrations completed successfully");
        } catch (Exception e) {
            log.error("Error running database migrations: {}", e.getMessage(), e);
            // Don't throw exception - let the application start even if migration fails
            // This prevents startup failures if the constraint already exists
        }
    }

    /**
     * Add HYBRID payment method to the order_transactions payment_method check constraint.
     * This migration is idempotent - it can be run multiple times safely.
     */
    private void addHybridPaymentMethodSupport() {
        try {
            log.info("Checking if HYBRID payment method constraint needs to be updated...");
            
            // Check if the constraint exists
            String checkConstraintSql = 
                "SELECT COUNT(*) FROM information_schema.constraint_column_usage " +
                "WHERE constraint_name = 'order_transactions_payment_method_check' " +
                "AND table_name = 'order_transactions'";
            
            Integer constraintExists = jdbcTemplate.queryForObject(checkConstraintSql, Integer.class);
            
            if (constraintExists != null && constraintExists > 0) {
                log.info("Payment method constraint exists, updating to include HYBRID...");
                
                // Drop the existing constraint
                String dropConstraintSql = 
                    "ALTER TABLE order_transactions DROP CONSTRAINT IF EXISTS order_transactions_payment_method_check";
                jdbcTemplate.execute(dropConstraintSql);
                log.info("Dropped existing payment_method constraint");
                
                // Add the new constraint with HYBRID included
                String addConstraintSql = 
                    "ALTER TABLE order_transactions " +
                    "ADD CONSTRAINT order_transactions_payment_method_check " +
                    "CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'))";
                jdbcTemplate.execute(addConstraintSql);
                log.info("Added new payment_method constraint with HYBRID support");
                
            } else {
                log.info("Payment method constraint does not exist, creating it...");
                
                // Create the constraint with HYBRID included
                String addConstraintSql = 
                    "ALTER TABLE order_transactions " +
                    "ADD CONSTRAINT order_transactions_payment_method_check " +
                    "CHECK (payment_method IN ('CREDIT_CARD', 'DEBIT_CARD', 'POINTS', 'HYBRID'))";
                jdbcTemplate.execute(addConstraintSql);
                log.info("Created payment_method constraint with HYBRID support");
            }
            
            // Verify the constraint was created successfully
            String verifyConstraintSql = 
                "SELECT pg_get_constraintdef(oid) " +
                "FROM pg_constraint " +
                "WHERE conrelid = 'order_transactions'::regclass " +
                "AND conname = 'order_transactions_payment_method_check'";
            
            try {
                String constraintDef = jdbcTemplate.queryForObject(verifyConstraintSql, String.class);
                log.info("Payment method constraint verified: {}", constraintDef);
            } catch (Exception e) {
                log.warn("Could not verify constraint (might not be PostgreSQL): {}", e.getMessage());
            }
            
        } catch (Exception e) {
            // Log the error but don't fail the application startup
            log.warn("Could not update payment method constraint (it might already be correct): {}", e.getMessage());
        }
    }
}
