package com.ecommerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Database Index Runner
 * 
 * This component runs on application startup to ensure all necessary database indexes
 * are created for optimal product search and filtering performance.
 * 
 * Features:
 * - Executes SQL scripts from resources/db/migration/
 * - Creates indexes for products, variants, stock, and related entities
 * - Handles full-text search indexes for PostgreSQL
 * - Optimizes query performance for e-commerce operations
 * - Runs only once per application startup
 * - Handles errors gracefully without stopping application startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run early in the startup process
public class DatabaseIndexRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    /**
     * List of SQL migration files to execute
     * Add new migration files here in order
     */
    private static final String[] MIGRATION_FILES = {
        "db/migration/V1__Create_Product_Indexes.sql",
        "db/migration/V2__Advanced_Search_Indexes.sql"
    };

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🚀 Starting Database Index Creation...");
        
        try {
            // Check if indexes already exist to avoid duplicate creation
            if (shouldSkipIndexCreation()) {
                log.info("✅ Database indexes already exist, skipping creation");
                return;
            }

            // Execute each migration file
            for (String migrationFile : MIGRATION_FILES) {
                executeMigrationFile(migrationFile);
            }

            // Mark indexes as created
            markIndexesAsCreated();
            
            log.info("✅ Database Index Creation completed successfully!");
            
        } catch (Exception e) {
            log.error("❌ Error during database index creation: {}", e.getMessage(), e);
            // Don't throw exception to avoid stopping application startup
            // Indexes are performance optimization, not critical for functionality
        }
    }

    /**
     * Execute a single migration file
     */
    private void executeMigrationFile(String migrationFile) {
        try {
            log.info("📄 Executing migration file: {}", migrationFile);
            
            String sqlContent = readSqlFile(migrationFile);
            List<String> sqlStatements = splitSqlStatements(sqlContent);
            
            int successCount = 0;
            int errorCount = 0;
            
            for (String statement : sqlStatements) {
                try {
                    if (!statement.trim().isEmpty() && !statement.trim().startsWith("--")) {
                        jdbcTemplate.execute(statement);
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.warn("⚠️ Failed to execute SQL statement: {} - Error: {}", 
                            statement.substring(0, Math.min(100, statement.length())), 
                            e.getMessage());
                }
            }
            
            log.info("✅ Migration file {} completed - Success: {}, Errors: {}", 
                    migrationFile, successCount, errorCount);
                    
        } catch (Exception e) {
            log.error("❌ Failed to execute migration file {}: {}", migrationFile, e.getMessage());
        }
    }

    /**
     * Read SQL file from classpath
     */
    private String readSqlFile(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        
        if (!resource.exists()) {
            throw new IOException("Migration file not found: " + filePath);
        }
        
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        return content.toString();
    }

    /**
     * Split SQL content into individual statements
     */
    private List<String> splitSqlStatements(String sqlContent) {
        List<String> statements = new ArrayList<>();
        
        // Split by semicolon, but be careful with comments and strings
        String[] parts = sqlContent.split(";");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                statements.add(trimmed);
            }
        }
        
        return statements;
    }

    /**
     * Check if indexes have already been created
     */
    private boolean shouldSkipIndexCreation() {
        try {
            // Check if our marker table exists
            String checkTableSql = """
                SELECT EXISTS (
                    SELECT FROM information_schema.tables 
                    WHERE table_schema = 'public' 
                    AND table_name = 'database_migrations'
                )
                """;
            
            Boolean tableExists = jdbcTemplate.queryForObject(checkTableSql, Boolean.class);
            
            if (Boolean.TRUE.equals(tableExists)) {
                // Check if all migrations have been run
                for (String migrationFile : MIGRATION_FILES) {
                    String migrationName = extractMigrationName(migrationFile);
                    String checkMigrationSql = """
                        SELECT EXISTS (
                            SELECT 1 FROM database_migrations 
                            WHERE migration_name = ?
                        )
                        """;
                    
                    Boolean migrationExists = jdbcTemplate.queryForObject(checkMigrationSql, Boolean.class, migrationName);
                    if (!Boolean.TRUE.equals(migrationExists)) {
                        log.info("Migration {} not found, proceeding with index creation", migrationName);
                        return false;
                    }
                }
                
                log.info("All migrations have been executed, skipping index creation");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.debug("Migration tracking table doesn't exist yet, proceeding with index creation");
            return false;
        }
    }

    /**
     * Extract migration name from file path
     */
    private String extractMigrationName(String filePath) {
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        return fileName.replace(".sql", "");
    }

    /**
     * Mark indexes as created in tracking table
     */
    private void markIndexesAsCreated() {
        try {
            // Create migration tracking table if it doesn't exist
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS database_migrations (
                    id SERIAL PRIMARY KEY,
                    migration_name VARCHAR(255) UNIQUE NOT NULL,
                    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    description TEXT
                )
                """;
            
            jdbcTemplate.execute(createTableSql);
            
            // Insert migration records for all executed files
            String insertMigrationSql = """
                INSERT INTO database_migrations (migration_name, description) 
                VALUES (?, ?) 
                ON CONFLICT (migration_name) DO NOTHING
                """;
            
            for (String migrationFile : MIGRATION_FILES) {
                String migrationName = extractMigrationName(migrationFile);
                String description = getMigrationDescription(migrationName);
                
                jdbcTemplate.update(insertMigrationSql, migrationName, description);
                log.debug("📝 Marked migration {} as completed", migrationName);
            }
                
            log.info("📝 Marked all index migrations as completed in migration tracking");
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to mark migrations as completed: {}", e.getMessage());
        }
    }

    /**
     * Get description for migration
     */
    private String getMigrationDescription(String migrationName) {
        return switch (migrationName) {
            case "V1__Create_Product_Indexes" -> "Created comprehensive indexes for product-related entities";
            case "V2__Advanced_Search_Indexes" -> "Created advanced search and performance optimization indexes";
            default -> "Database index migration: " + migrationName;
        };
    }

    /**
     * Verify that critical indexes were created successfully
     */
    public boolean verifyIndexes() {
        try {
            log.info("🔍 Verifying critical database indexes...");
            
            String[] criticalIndexes = {
                "idx_products_name_search",
                "idx_products_category_id", 
                "idx_products_price",
                "idx_products_active",
                "idx_product_details_product_id",
                "idx_stocks_product_id"
            };
            
            int foundIndexes = 0;
            
            for (String indexName : criticalIndexes) {
                String checkIndexSql = """
                    SELECT EXISTS (
                        SELECT 1 FROM pg_indexes 
                        WHERE indexname = ?
                    )
                    """;
                
                Boolean indexExists = jdbcTemplate.queryForObject(checkIndexSql, Boolean.class, indexName);
                
                if (Boolean.TRUE.equals(indexExists)) {
                    foundIndexes++;
                    log.debug("✅ Index found: {}", indexName);
                } else {
                    log.warn("❌ Index missing: {}", indexName);
                }
            }
            
            boolean allIndexesFound = foundIndexes == criticalIndexes.length;
            
            if (allIndexesFound) {
                log.info("✅ All critical indexes verified successfully ({}/{})", 
                        foundIndexes, criticalIndexes.length);
            } else {
                log.warn("⚠️ Some indexes are missing ({}/{})", 
                        foundIndexes, criticalIndexes.length);
            }
            
            return allIndexesFound;
            
        } catch (Exception e) {
            log.error("❌ Error verifying indexes: {}", e.getMessage());
            return false;
        }
    }
}
