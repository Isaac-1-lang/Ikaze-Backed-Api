package com.ecommerce.service;

import com.ecommerce.dto.PointsPaymentPreviewDTO;
import com.ecommerce.dto.PointsPaymentRequest;
import com.ecommerce.dto.PointsPaymentResult;

import java.util.UUID;

public interface PointsPaymentService {

    PointsPaymentPreviewDTO previewPointsPayment(PointsPaymentRequest request);

    PointsPaymentResult processPointsPayment(PointsPaymentRequest request);

    PointsPaymentResult completeHybridPayment(UUID userId, Long orderId, String stripeSessionId);

    com.ecommerce.dto.PointsEligibilityResponse checkPointsEligibility(
            com.ecommerce.dto.PointsEligibilityRequest request);
}
