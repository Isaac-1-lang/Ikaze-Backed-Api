package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.SystemResetRequest;
import com.ecommerce.dto.SystemResetResponse;
import com.ecommerce.dto.SystemResetResponse.SystemResetStats;
import com.ecommerce.repository.*;
import com.ecommerce.service.SystemResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemResetServiceImpl implements SystemResetService {

    private final ProductRepository productRepository;
    private final DiscountRepository discountRepository;
    private final OrderRepository orderRepository;
    private final RewardSystemRepository rewardSystemRepository;
    private final ShippingCostRepository shippingCostRepository;
    private final MoneyFlowRepository moneyFlowRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final WarehouseRepository warehouseRepository;
    
    // Additional repositories for cascading deletes
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVideoRepository productVideoRepository;
    private final StockRepository stockRepository;
    private final StockBatchRepository stockBatchRepository;
    private final CartItemRepository cartItemRepository;
    private final ReviewRepository reviewRepository;
    private final RewardRangeRepository rewardRangeRepository;
    private final UserPointsRepository userPointsRepository;
    
    // Order-related repositories
    private final OrderDeliveryNoteRepository orderDeliveryNoteRepository;
    private final OrderTrackingTokenRepository orderTrackingTokenRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnMediaRepository returnMediaRepository;
    private final ReturnAppealRepository returnAppealRepository;

    @Override
    public SystemResetResponse performSystemReset(SystemResetRequest request) {
        log.info("Starting system reset operation");
        long startTime = System.currentTimeMillis();
        
        SystemResetResponse response = SystemResetResponse.builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .errors(new ArrayList<>())
                .build();
        
        SystemResetStats stats = SystemResetStats.builder()
                .productsDeleted(0)
                .discountsDeleted(0)
                .ordersDeleted(0)
                .rewardSystemsDeleted(0)
                .shippingCostsDeleted(0)
                .moneyFlowsDeleted(0)
                .categoriesDeleted(0)
                .brandsDeleted(0)
                .warehousesDeleted(0)
                .totalDeleted(0)
                .build();
        
        // Create thread pool for parallel execution
        int threadPoolSize = Math.min(9, Runtime.getRuntime().availableProcessors());
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<DeletionResult>> futures = new ArrayList<>();
        
        try {
            // Submit deletion tasks based on request flags
            if (Boolean.TRUE.equals(request.getDeleteProducts())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Products", this::deleteAllProducts)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteDiscounts())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Discounts", this::deleteAllDiscounts)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteOrders())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Orders", this::deleteAllOrders)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteRewardSystems())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("RewardSystems", this::deleteAllRewardSystems)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteShippingCosts())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("ShippingCosts", this::deleteAllShippingCosts)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteMoneyFlows())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("MoneyFlows", this::deleteAllMoneyFlows)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteCategories())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Categories", this::deleteAllCategories)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteBrands())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Brands", this::deleteAllBrands)));
            }
            
            if (Boolean.TRUE.equals(request.getDeleteWarehouses())) {
                futures.add(executorService.submit(() -> 
                    executeDeletion("Warehouses", this::deleteAllWarehouses)));
            }
            
            // Collect results from all tasks
            for (Future<DeletionResult> future : futures) {
                try {
                    DeletionResult result = future.get(5, TimeUnit.MINUTES);
                    
                    // Update stats based on entity type
                    switch (result.getEntityType()) {
                        case "Products":
                            stats.setProductsDeleted(result.getCount());
                            break;
                        case "Discounts":
                            stats.setDiscountsDeleted(result.getCount());
                            break;
                        case "Orders":
                            stats.setOrdersDeleted(result.getCount());
                            break;
                        case "RewardSystems":
                            stats.setRewardSystemsDeleted(result.getCount());
                            break;
                        case "ShippingCosts":
                            stats.setShippingCostsDeleted(result.getCount());
                            break;
                        case "MoneyFlows":
                            stats.setMoneyFlowsDeleted(result.getCount());
                            break;
                        case "Categories":
                            stats.setCategoriesDeleted(result.getCount());
                            break;
                        case "Brands":
                            stats.setBrandsDeleted(result.getCount());
                            break;
                        case "Warehouses":
                            stats.setWarehousesDeleted(result.getCount());
                            break;
                    }
                    
                    // Add error if present
                    if (result.getError() != null) {
                        response.addError(result.getEntityType(), result.getError(), result.getErrorDetails());
                    }
                    
                } catch (TimeoutException e) {
                    log.error("Timeout while deleting entities", e);
                    response.addError("System", "Timeout", "Operation took too long to complete");
                } catch (Exception e) {
                    log.error("Error collecting deletion results", e);
                    response.addError("System", "Collection Error", e.getMessage());
                }
            }
            
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(6, TimeUnit.MINUTES)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Calculate totals
        stats.setTotalDeleted(
            stats.getProductsDeleted() +
            stats.getDiscountsDeleted() +
            stats.getOrdersDeleted() +
            stats.getRewardSystemsDeleted() +
            stats.getShippingCostsDeleted() +
            stats.getMoneyFlowsDeleted() +
            stats.getCategoriesDeleted() +
            stats.getBrandsDeleted() +
            stats.getWarehousesDeleted()
        );
        
        long endTime = System.currentTimeMillis();
        stats.setExecutionTimeMs(endTime - startTime);
        
        response.setStats(stats);
        response.setMessage(String.format("System reset completed. Total entities deleted: %d", stats.getTotalDeleted()));
        
        log.info("System reset completed in {}ms. Total deleted: {}", stats.getExecutionTimeMs(), stats.getTotalDeleted());
        
        return response;
    }
    
    /**
     * Helper method to execute deletion with error handling
     */
    private DeletionResult executeDeletion(String entityType, DeletionTask task) {
        try {
            log.info("Starting deletion of {}", entityType);
            long count = task.execute();
            log.info("Successfully deleted {} {}", count, entityType);
            return new DeletionResult(entityType, count, null, null);
        } catch (Exception e) {
            log.error("Error deleting {}: {}", entityType, e.getMessage(), e);
            return new DeletionResult(entityType, 0, e.getMessage(), e.getClass().getSimpleName());
        }
    }
    
    @Override
    @Transactional
    public long deleteAllProducts() {
        log.info("Deleting all products with cascading relationships");
        
        try {
            // First, remove product associations from carts and wishlists
            cartItemRepository.deleteAll();
            log.info("Deleted all cart items");
            
            // Delete reviews
            reviewRepository.deleteAll();
            log.info("Deleted all reviews");
            
            // Delete stock batches first
            stockBatchRepository.deleteAll();
            log.info("Deleted all stock batches");
            
            // Delete stocks
            stockRepository.deleteAll();
            log.info("Deleted all stocks");
            
            // Delete product variant images
            productVariantRepository.findAll().forEach(variant -> {
                variant.getImages().clear();
            });
            productVariantRepository.flush();
            
            // Delete product variants
            productVariantRepository.deleteAll();
            log.info("Deleted all product variants");
            
            // Delete product images
            productImageRepository.deleteAll();
            log.info("Deleted all product images");
            
            // Delete product videos
            productVideoRepository.deleteAll();
            log.info("Deleted all product videos");
            
            // Finally delete products
            long count = productRepository.count();
            productRepository.deleteAll();
            log.info("Deleted {} products", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during product deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete products: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllDiscounts() {
        log.info("Deleting all discounts with cascading relationships");
        
        try {
            // Remove discount associations from products and variants
            productRepository.findAll().forEach(product -> {
                product.setDiscount(null);
            });
            productRepository.flush();
            
            productVariantRepository.findAll().forEach(variant -> {
                variant.setDiscount(null);
            });
            productVariantRepository.flush();
            
            long count = discountRepository.count();
            discountRepository.deleteAll();
            log.info("Deleted {} discounts", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during discount deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete discounts: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllOrders() {
        log.info("Deleting all orders with cascading relationships");
        
        try {
            // Step 1: Delete return appeals (references return requests)
            long appealCount = returnAppealRepository.count();
            returnAppealRepository.deleteAll();
            log.info("Deleted {} return appeals", appealCount);
            
            // Step 2: Delete return media (references return requests)
            long mediaCount = returnMediaRepository.count();
            returnMediaRepository.deleteAll();
            log.info("Deleted {} return media", mediaCount);
            
            // Step 3: Delete return items (references return requests)
            long returnItemCount = returnItemRepository.count();
            returnItemRepository.deleteAll();
            log.info("Deleted {} return items", returnItemCount);
            
            // Step 4: Delete return requests (references orders)
            long returnRequestCount = returnRequestRepository.count();
            returnRequestRepository.deleteAll();
            log.info("Deleted {} return requests", returnRequestCount);
            
            // Step 5: Delete order delivery notes (references orders)
            long deliveryNoteCount = orderDeliveryNoteRepository.count();
            orderDeliveryNoteRepository.deleteAll();
            log.info("Deleted {} order delivery notes", deliveryNoteCount);
            
            // Step 6: Delete order tracking tokens (references orders)
            long trackingTokenCount = orderTrackingTokenRepository.count();
            orderTrackingTokenRepository.deleteAll();
            log.info("Deleted {} order tracking tokens", trackingTokenCount);
            
            // Step 7: Delete order item batches (references order items)
            long itemBatchCount = orderItemBatchRepository.count();
            orderItemBatchRepository.deleteAll();
            log.info("Deleted {} order item batches", itemBatchCount);
            
            // Step 8: Now delete orders (JPA cascading will handle OrderItems, OrderTransaction, 
            // OrderAddress, OrderCustomerInfo, OrderInfo due to CascadeType.ALL and orphanRemoval)
            long count = orderRepository.count();
            orderRepository.deleteAll();
            log.info("Deleted {} orders with all cascading relationships", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during order deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete orders: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllRewardSystems() {
        log.info("Deleting all reward systems with cascading relationships");
        
        try {
            // Delete reward ranges first (they reference reward systems)
            rewardRangeRepository.deleteAll();
            log.info("Deleted all reward ranges");
            
            // Delete user points
            userPointsRepository.deleteAll();
            log.info("Deleted all user points");
            
            long count = rewardSystemRepository.count();
            rewardSystemRepository.deleteAll();
            log.info("Deleted {} reward systems", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during reward system deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete reward systems: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllShippingCosts() {
        log.info("Deleting all shipping costs");
        
        try {
            long count = shippingCostRepository.count();
            shippingCostRepository.deleteAll();
            log.info("Deleted {} shipping costs", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during shipping cost deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete shipping costs: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllMoneyFlows() {
        log.info("Deleting all money flow records");
        
        try {
            long count = moneyFlowRepository.count();
            moneyFlowRepository.deleteAll();
            log.info("Deleted {} money flow records", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during money flow deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete money flows: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllCategories() {
        log.info("Deleting all categories with cascading relationships");
        
        try {
            // Remove category associations from products
            productRepository.findAll().forEach(product -> {
                product.setCategory(null);
            });
            productRepository.flush();
            
            long count = categoryRepository.count();
            categoryRepository.deleteAll();
            log.info("Deleted {} categories", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during category deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete categories: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllBrands() {
        log.info("Deleting all brands with cascading relationships");
        
        try {
            // Remove brand associations from products
            productRepository.findAll().forEach(product -> {
                product.setBrand(null);
            });
            productRepository.flush();
            
            long count = brandRepository.count();
            brandRepository.deleteAll();
            log.info("Deleted {} brands", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during brand deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete brands: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public long deleteAllWarehouses() {
        log.info("Deleting all warehouses with cascading relationships");
        
        try {
            // Delete stock batches first
            stockBatchRepository.deleteAll();
            log.info("Deleted all stock batches");
            
            // Delete stocks
            stockRepository.deleteAll();
            log.info("Deleted all stocks");
            
            // JPA cascading will handle WarehouseImages due to CascadeType.ALL and orphanRemoval
            long count = warehouseRepository.count();
            warehouseRepository.deleteAll();
            log.info("Deleted {} warehouses with all cascading relationships", count);
            
            return count;
        } catch (Exception e) {
            log.error("Error during warehouse deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete warehouses: " + e.getMessage(), e);
        }
    }
    
    /**
     * Functional interface for deletion tasks
     */
    @FunctionalInterface
    private interface DeletionTask {
        long execute() throws Exception;
    }
    
    /**
     * Result class for deletion operations
     */
    private static class DeletionResult {
        private final String entityType;
        private final long count;
        private final String error;
        private final String errorDetails;
        
        public DeletionResult(String entityType, long count, String error, String errorDetails) {
            this.entityType = entityType;
            this.count = count;
            this.error = error;
            this.errorDetails = errorDetails;
        }
        
        public String getEntityType() {
            return entityType;
        }
        
        public long getCount() {
            return count;
        }
        
        public String getError() {
            return error;
        }
        
        public String getErrorDetails() {
            return errorDetails;
        }
    }
}
