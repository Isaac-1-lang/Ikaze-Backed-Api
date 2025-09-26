package com.ecommerce.service;

import com.ecommerce.dto.*;
import com.ecommerce.entity.*;
import com.ecommerce.repository.*;
import com.ecommerce.service.impl.PointsPaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsPaymentServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private OrderTransactionRepository transactionRepository;
    
    @Mock
    private RewardService rewardService;
    
    @Mock
    private ShippingCostService shippingCostService;
    
    @Mock
    private EnhancedStockValidationService stockValidationService;
    
    @Mock
    private EnhancedMultiWarehouseAllocator warehouseAllocator;
    
    @Mock
    private FEFOStockAllocationService fefoService;
    
    @Mock
    private OrderItemBatchRepository orderItemBatchRepository;
    
    @Mock
    private StripeService stripeService;

    @InjectMocks
    private PointsPaymentServiceImpl pointsPaymentService;

    private User testUser;
    private PointsPaymentRequest testRequest;
    private RewardSystemDTO testRewardSystem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUserEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPoints(1000);

        CartItemDTO cartItem = new CartItemDTO();
        cartItem.setProductId(UUID.randomUUID());
        cartItem.setQuantity(2);
        cartItem.setPrice(BigDecimal.valueOf(25.00));

        AddressDto address = new AddressDto();
        address.setStreetAddress("123 Test St");
        address.setCity("Test City");
        address.setState("Test State");
        address.setCountry("Test Country");

        testRequest = new PointsPaymentRequest();
        testRequest.setUserId(testUser.getId());
        testRequest.setItems(Arrays.asList(cartItem));
        testRequest.setShippingAddress(address);
        testRequest.setUseAllAvailablePoints(true);

        testRewardSystem = new RewardSystemDTO();
        testRewardSystem.setId(1L);
        testRewardSystem.setPointValue(BigDecimal.valueOf(0.01));
        testRewardSystem.setIsSystemEnabled(true);
    }

    @Test
    void testPreviewPointsPayment_SufficientPoints() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(rewardService.calculatePointsValue(1000)).thenReturn(BigDecimal.valueOf(10.00));
        when(rewardService.getActiveRewardSystem()).thenReturn(testRewardSystem);
        when(shippingCostService.calculateOrderShippingCost(any(), any(), any())).thenReturn(BigDecimal.valueOf(5.00));

        PointsPaymentPreviewDTO preview = pointsPaymentService.previewPointsPayment(testRequest);

        assertNotNull(preview);
        assertEquals(BigDecimal.valueOf(55.00), preview.getTotalAmount());
        assertEquals(Integer.valueOf(1000), preview.getAvailablePoints());
        assertEquals(BigDecimal.valueOf(10.00), preview.getPointsValue());
        assertEquals(BigDecimal.valueOf(45.00), preview.getRemainingToPay());
        assertFalse(preview.isCanPayWithPointsOnly());
    }

    @Test
    void testPreviewPointsPayment_InsufficientPoints() {
        testUser.setPoints(100);
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(rewardService.calculatePointsValue(100)).thenReturn(BigDecimal.valueOf(1.00));
        when(rewardService.getActiveRewardSystem()).thenReturn(testRewardSystem);
        when(shippingCostService.calculateOrderShippingCost(any(), any(), any())).thenReturn(BigDecimal.valueOf(5.00));

        PointsPaymentPreviewDTO preview = pointsPaymentService.previewPointsPayment(testRequest);

        assertNotNull(preview);
        assertEquals(BigDecimal.valueOf(55.00), preview.getTotalAmount());
        assertEquals(Integer.valueOf(100), preview.getAvailablePoints());
        assertEquals(BigDecimal.valueOf(1.00), preview.getPointsValue());
        assertEquals(BigDecimal.valueOf(54.00), preview.getRemainingToPay());
        assertFalse(preview.isCanPayWithPointsOnly());
    }

    @Test
    void testPreviewPointsPayment_ExactPoints() {
        testUser.setPoints(5500);
        
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(rewardService.calculatePointsValue(5500)).thenReturn(BigDecimal.valueOf(55.00));
        when(rewardService.getActiveRewardSystem()).thenReturn(testRewardSystem);
        when(shippingCostService.calculateOrderShippingCost(any(), any(), any())).thenReturn(BigDecimal.valueOf(5.00));

        PointsPaymentPreviewDTO preview = pointsPaymentService.previewPointsPayment(testRequest);

        assertNotNull(preview);
        assertEquals(BigDecimal.valueOf(55.00), preview.getTotalAmount());
        assertEquals(Integer.valueOf(5500), preview.getAvailablePoints());
        assertEquals(BigDecimal.valueOf(55.00), preview.getPointsValue());
        assertEquals(BigDecimal.valueOf(0.00), preview.getRemainingToPay());
        assertTrue(preview.isCanPayWithPointsOnly());
    }

    @Test
    void testPreviewPointsPayment_UserNotFound() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            pointsPaymentService.previewPointsPayment(testRequest);
        });
    }
}
