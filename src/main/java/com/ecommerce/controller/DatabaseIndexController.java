package com.ecommerce.controller;

import com.ecommerce.config.DatabaseIndexRunner;
import com.ecommerce.service.DatabaseIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database Index Management Controller
 * 
 * Provides REST endpoints for managing and monitoring database indexes.
 * Only accessible by ADMIN users for security.
 */
@RestController
@RequestMapping("/api/admin/database")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DatabaseIndexController {

    private final DatabaseIndexService databaseIndexService;
    private final DatabaseIndexRunner databaseIndexRunner;

    /**
     * Get comprehensive database index information
     */
    @GetMapping("/indexes")
    public ResponseEntity<Map<String, Object>> getIndexInformation() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            response.put("indexes", databaseIndexService.getProductIndexInfo());
            response.put("usage_stats", databaseIndexService.getIndexUsageStats());
            response.put("table_sizes", databaseIndexService.getTableSizeInfo());
            response.put("performance_metrics", databaseIndexService.getDatabasePerformanceMetrics());
            response.put("suggestions", databaseIndexService.suggestMissingIndexes());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving index information: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to retrieve index information"));
        }
    }

    /**
     * Get index usage statistics
     */
    @GetMapping("/indexes/usage")
    public ResponseEntity<List<Map<String, Object>>> getIndexUsageStats() {
        try {
            List<Map<String, Object>> stats = databaseIndexService.getIndexUsageStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving index usage stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get table size information
     */
    @GetMapping("/tables/sizes")
    public ResponseEntity<List<Map<String, Object>>> getTableSizes() {
        try {
            List<Map<String, Object>> sizes = databaseIndexService.getTableSizeInfo();
            return ResponseEntity.ok(sizes);
            
        } catch (Exception e) {
            log.error("Error retrieving table sizes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get slow queries related to products
     */
    @GetMapping("/queries/slow")
    public ResponseEntity<List<Map<String, Object>>> getSlowQueries() {
        try {
            List<Map<String, Object>> slowQueries = databaseIndexService.getSlowProductQueries();
            return ResponseEntity.ok(slowQueries);
            
        } catch (Exception e) {
            log.error("Error retrieving slow queries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get database performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        try {
            Map<String, Object> metrics = databaseIndexService.getDatabasePerformanceMetrics();
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Error retrieving performance metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verify that critical indexes exist
     */
    @GetMapping("/indexes/verify")
    public ResponseEntity<Map<String, Object>> verifyIndexes() {
        try {
            boolean allIndexesExist = databaseIndexRunner.verifyIndexes();
            
            Map<String, Object> response = new HashMap<>();
            response.put("all_indexes_exist", allIndexesExist);
            response.put("message", allIndexesExist ? 
                "All critical indexes are present" : 
                "Some critical indexes are missing");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error verifying indexes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to verify indexes"));
        }
    }

    /**
     * Analyze tables to update query planner statistics
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeTables() {
        try {
            databaseIndexService.analyzeProductTables();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Table analysis completed successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error analyzing tables: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to analyze tables"));
        }
    }

    /**
     * Reindex product tables
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, String>> reindexTables() {
        try {
            databaseIndexService.reindexProductTables();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Table reindexing completed successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error reindexing tables: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reindex tables"));
        }
    }

    /**
     * Perform database maintenance tasks
     */
    @PostMapping("/maintenance")
    public ResponseEntity<Map<String, String>> performMaintenance() {
        try {
            databaseIndexService.performMaintenanceTasks();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Database maintenance completed successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error performing maintenance: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to perform maintenance"));
        }
    }

    /**
     * Get suggestions for missing indexes
     */
    @GetMapping("/indexes/suggestions")
    public ResponseEntity<Map<String, Object>> getIndexSuggestions() {
        try {
            List<String> suggestions = databaseIndexService.suggestMissingIndexes();
            
            Map<String, Object> response = new HashMap<>();
            response.put("suggestions", suggestions);
            response.put("count", suggestions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting index suggestions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get index suggestions"));
        }
    }

    /**
     * Health check for database indexing system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Check if critical indexes exist
            boolean indexesExist = databaseIndexRunner.verifyIndexes();
            
            // Get basic metrics
            Map<String, Object> metrics = databaseIndexService.getDatabasePerformanceMetrics();
            
            health.put("status", indexesExist ? "healthy" : "degraded");
            health.put("indexes_verified", indexesExist);
            health.put("active_products", metrics.get("active_products"));
            health.put("total_variants", metrics.get("total_variants"));
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Error checking database health: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "status", "error",
                        "error", "Failed to check database health"
                    ));
        }
    }
}
