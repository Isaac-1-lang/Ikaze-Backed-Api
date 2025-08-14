package com.ecommerce.service;

import com.ecommerce.dto.AnalyticsRequestDTO;
import com.ecommerce.dto.AnalyticsResponseDTO;

public interface AnalyticsService {
    AnalyticsResponseDTO getAnalytics(AnalyticsRequestDTO request, String bearerToken);
}
