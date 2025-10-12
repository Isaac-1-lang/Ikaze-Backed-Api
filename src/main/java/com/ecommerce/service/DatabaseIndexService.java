package com.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Database Index Management Service
 * 
 * Provides functionality to manage, monitor, and optimize database indexes
 * for product-related entities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseIndexService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get information about all indexes on product-related tables
     */
    public List<Map<String, Object>> getProductIndexInfo() {
        String sql = """
            SELECT 
                schemaname,
                tablename,
                indexname,
                indexdef,
                CASE 
                    WHEN indexdef LIKE '%UNIQUE%' THEN 'UNIQUE'
                    WHEN indexdef LIKE '%gin%' THEN 'GIN'
                    WHEN indexdef LIKE '%btree%' THEN 'BTREE'
                    ELSE 'OTHER'
                END as index_type
            FROM pg_indexes 
            WHERE tablename IN (
                'products', 
                'product_details', 
                'product_variants', 
                'stocks', 
                'product_images', 
                'product_videos',
                'reviews',
                'categories',
                'brands'
            )
            ORDER BY tablename, indexname
            """;
        
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get index usage statistics
     */
    public List<Map<String, Object>> getIndexUsageStats() {
        String sql = """
            SELECT 
                schemaname,
                tablename,
                indexname,
                idx_tup_read,
                idx_tup_fetch,
                idx_scan,
                CASE 
                    WHEN idx_scan = 0 THEN 'UNUSED'
                    WHEN idx_scan < 100 THEN 'LOW_USAGE'
                    WHEN idx_scan < 1000 THEN 'MEDIUM_USAGE'
                    ELSE 'HIGH_USAGE'
                END as usage_level
            FROM pg_stat_user_indexes 
            WHERE schemaname = 'public'
            AND relname IN (
                'products', 
                'product_details', 
                'product_variants', 
                'stocks', 
                'product_images', 
                'product_videos',
                'reviews',
                'categories',
                'brands'
            )
            ORDER BY idx_scan DESC
            """;
        
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Get table size and index size information
     */
    public List<Map<String, Object>> getTableSizeInfo() {
        String sql = """
            SELECT 
                schemaname,
                tablename,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
                pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
                pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size,
                pg_stat_get_tuples_returned(c.oid) as tuples_returned,
                pg_stat_get_tuples_fetched(c.oid) as tuples_fetched
            FROM pg_tables pt
            JOIN pg_class c ON c.relname = pt.tablename
            WHERE pt.schemaname = 'public'
            AND pt.tablename IN (
                'products', 
                'product_details', 
                'product_variants', 
                'stocks', 
                'product_images', 
                'product_videos',
                'reviews',
                'categories',
                'brands'
            )
            ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC
            """;
        
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Analyze tables to update statistics for query planner
     */
    @Transactional
    public void analyzeProductTables() {
        log.info("🔍 Analyzing product-related tables for query optimization...");
        
        String[] tables = {
            "products", 
            "product_details", 
            "product_variants", 
            "stocks", 
            "product_images", 
            "product_videos",
            "reviews",
            "categories",
            "brands"
        };
        
        for (String table : tables) {
            try {
                jdbcTemplate.execute("ANALYZE " + table);
                log.debug("✅ Analyzed table: {}", table);
            } catch (Exception e) {
                log.warn("⚠️ Failed to analyze table {}: {}", table, e.getMessage());
            }
        }
        
        log.info("✅ Table analysis completed");
    }

    /**
     * Reindex specific tables if needed
     */
    @Transactional
    public void reindexProductTables() {
        log.info("🔄 Reindexing product-related tables...");
        
        String[] tables = {
            "products", 
            "product_details", 
            "product_variants", 
            "stocks"
        };
        
        for (String table : tables) {
            try {
                jdbcTemplate.execute("REINDEX TABLE " + table);
                log.debug("✅ Reindexed table: {}", table);
            } catch (Exception e) {
                log.warn("⚠️ Failed to reindex table {}: {}", table, e.getMessage());
            }
        }
        
        log.info("✅ Table reindexing completed");
    }

    /**
     * Get slow query information related to products
     */
    public List<Map<String, Object>> getSlowProductQueries() {
        // Note: This requires pg_stat_statements extension to be enabled
        String sql = """
            SELECT 
                query,
                calls,
                total_time,
                mean_time,
                rows,
                100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
            FROM pg_stat_statements 
            WHERE query ILIKE '%products%' 
               OR query ILIKE '%product_details%'
               OR query ILIKE '%product_variants%'
               OR query ILIKE '%stocks%'
            ORDER BY mean_time DESC 
            LIMIT 20
            """;
        
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.warn("pg_stat_statements extension not available for slow query analysis");
            return List.of();
        }
    }

    /**
     * Check for missing indexes based on common query patterns
     */
    public List<String> suggestMissingIndexes() {
        List<String> suggestions = List.of();
        
        try {
            // Check for tables without proper indexes
            String checkMissingIndexes = """
                SELECT 
                    'Consider adding index on ' || schemaname || '.' || tablename || 
                    ' for column(s) that appear frequently in WHERE clauses' as suggestion
                FROM pg_stat_user_tables 
                WHERE schemaname = 'public'
                AND relname IN ('products', 'product_details', 'product_variants', 'stocks')
                AND seq_scan > idx_scan * 100  -- More sequential scans than index scans
                """;
            
            suggestions = jdbcTemplate.queryForList(checkMissingIndexes, String.class);
            
        } catch (Exception e) {
            log.warn("Could not analyze missing indexes: {}", e.getMessage());
        }
        
        return suggestions;
    }

    /**
     * Get database performance metrics
     */
    public Map<String, Object> getDatabasePerformanceMetrics() {
        String sql = """
            SELECT 
                (SELECT count(*) FROM products WHERE is_active = true) as active_products,
                (SELECT count(*) FROM product_variants) as total_variants,
                (SELECT count(*) FROM stocks WHERE quantity > 0) as in_stock_items,
                (SELECT count(*) FROM reviews) as total_reviews,
                (SELECT avg(rating) FROM reviews) as average_rating,
                (SELECT count(*) FROM product_images) as total_images
            """;
        
        return jdbcTemplate.queryForMap(sql);
    }

    /**
     * Vacuum and analyze all product tables for maintenance
     */
    @Transactional
    public void performMaintenanceTasks() {
        log.info("🧹 Performing database maintenance tasks...");
        
        String[] tables = {
            "products", 
            "product_details", 
            "product_variants", 
            "stocks", 
            "product_images", 
            "reviews"
        };
        
        for (String table : tables) {
            try {
                // Vacuum to reclaim space
                jdbcTemplate.execute("VACUUM " + table);
                // Analyze to update statistics
                jdbcTemplate.execute("ANALYZE " + table);
                log.debug("✅ Maintained table: {}", table);
            } catch (Exception e) {
                log.warn("⚠️ Failed to maintain table {}: {}", table, e.getMessage());
            }
        }
        
        log.info("✅ Database maintenance completed");
    }
}
