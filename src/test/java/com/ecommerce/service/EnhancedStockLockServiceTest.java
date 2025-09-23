// package com.ecommerce.service;

// import com.ecommerce.entity.StockBatch;
// import com.ecommerce.entity.StockBatchLock;
// import com.ecommerce.entity.Stock;
// import com.ecommerce.entity.Product;
// import com.ecommerce.entity.Warehouse;
// import com.ecommerce.repository.StockBatchLockRepository;
// import com.ecommerce.repository.StockBatchRepository;
// import com.ecommerce.repository.StockRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.*;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class EnhancedStockLockServiceTest {

//     @Mock
//     private StockBatchLockRepository stockBatchLockRepository;

//     @Mock
//     private StockBatchRepository stockBatchRepository;

//     @Mock
//     private StockRepository stockRepository;

//     @InjectMocks
//     private EnhancedStockLockService enhancedStockLockService;

//     private String sessionId;
//     private Stock mockStock;
//     private StockBatch mockBatch;
//     private MultiWarehouseStockAllocator.StockAllocation mockAllocation;

//     @BeforeEach
//     void setUp() {
//         sessionId = "test-session-123";
        
//         Product mockProduct = new Product();
//         mockProduct.setProductId(UUID.randomUUID());
//         mockProduct.setProductName("Test Product");
        
//         Warehouse mockWarehouse = new Warehouse();
//         mockWarehouse.setId(1L);
//         mockWarehouse.setName("Test Warehouse");
        
//         mockStock = new Stock();
//         mockStock.setId(1L);
//         mockStock.setProduct(mockProduct);
//         mockStock.setWarehouse(mockWarehouse);
//         mockStock.setQuantity(100);
        
//         mockBatch = new StockBatch();
//         mockBatch.setId(1L);
//         mockBatch.setStock(mockStock);
//         mockBatch.setBatchNumber("BATCH-001");
//         mockBatch.setQuantity(50);
        
//         mockAllocation = new MultiWarehouseStockAllocator.StockAllocation();
//         mockAllocation.setStockId(1L);
//         mockAllocation.setWarehouseId(1L);
//         mockAllocation.setWarehouseName("Test Warehouse");
//         mockAllocation.setQuantity(10);
//     }

//     @Test
//     void testLockStockFromBatches_Success() {
//         Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> allocations = new HashMap<>();
//         allocations.put(1L, Arrays.asList(mockAllocation));
        
//         when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));
//         when(stockBatchRepository.findActiveByStockIdOrderByExpiryDateAsc(1L))
//                 .thenReturn(Arrays.asList(mockBatch));
//         when(stockBatchLockRepository.getTotalLockedQuantityForBatch(1L)).thenReturn(0);
//         when(stockBatchLockRepository.saveAll(anyList())).thenReturn(new ArrayList<>());
        
//         boolean result = enhancedStockLockService.lockStockFromBatches(sessionId, allocations);
        
//         assertTrue(result);
//         verify(stockBatchLockRepository).saveAll(anyList());
//     }

//     @Test
//     void testLockStockFromBatches_InsufficientStock() {
//         mockAllocation.setQuantity(60);
//         Map<Long, List<MultiWarehouseStockAllocator.StockAllocation>> allocations = new HashMap<>();
//         allocations.put(1L, Arrays.asList(mockAllocation));
        
//         when(stockRepository.findById(1L)).thenReturn(Optional.of(mockStock));
//         when(stockBatchRepository.findActiveByStockIdOrderByExpiryDateAsc(1L))
//                 .thenReturn(Arrays.asList(mockBatch));
//         when(stockBatchLockRepository.getTotalLockedQuantityForBatch(1L)).thenReturn(0);
        
//         boolean result = enhancedStockLockService.lockStockFromBatches(sessionId, allocations);
        
//         assertFalse(result);
//         verify(stockBatchLockRepository).deleteBySessionId(sessionId);
//     }

//     @Test
//     void testUnlockAllBatches() {
//         StockBatchLock mockLock = new StockBatchLock();
//         mockLock.setSessionId(sessionId);
//         mockLock.setStockBatch(mockBatch);
//         mockLock.setLockedQuantity(10);
//         mockLock.setWarehouseName("Test Warehouse");
        
//         when(stockBatchLockRepository.findBySessionId(sessionId))
//                 .thenReturn(Arrays.asList(mockLock));
        
//         enhancedStockLockService.unlockAllBatches(sessionId);
        
//         verify(stockBatchLockRepository).deleteBySessionId(sessionId);
//     }

//     @Test
//     void testConfirmBatchLocks() {
//         StockBatchLock mockLock = new StockBatchLock();
//         mockLock.setSessionId(sessionId);
//         mockLock.setStockBatch(mockBatch);
//         mockLock.setLockedQuantity(10);
        
//         when(stockBatchLockRepository.findBySessionId(sessionId))
//                 .thenReturn(Arrays.asList(mockLock));
//         when(stockBatchRepository.findById(1L)).thenReturn(Optional.of(mockBatch));
        
//         enhancedStockLockService.confirmBatchLocks(sessionId);
        
//         verify(stockBatchRepository).save(mockBatch);
//         verify(stockBatchLockRepository).deleteBySessionId(sessionId);
//         assertEquals(40, mockBatch.getQuantity());
//     }

//     @Test
//     void testGetBatchLockInfo() {
//         StockBatchLock mockLock = new StockBatchLock();
//         mockLock.setSessionId(sessionId);
//         mockLock.setStockBatch(mockBatch);
//         mockLock.setLockedQuantity(10);
//         mockLock.setWarehouseName("Test Warehouse");
//         mockLock.setProductName("Test Product");
        
//         when(stockBatchLockRepository.findBySessionId(sessionId))
//                 .thenReturn(Arrays.asList(mockLock));
        
//         Map<String, Object> result = enhancedStockLockService.getBatchLockInfo(sessionId);
        
//         assertEquals(sessionId, result.get("sessionId"));
//         assertEquals(1, result.get("totalLocks"));
//         assertEquals(true, result.get("hasLocks"));
//         assertEquals(10, result.get("totalLockedQuantity"));
//         assertNotNull(result.get("locksByWarehouse"));
//     }
// }
