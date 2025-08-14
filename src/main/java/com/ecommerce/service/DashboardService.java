package com.ecommerce.service;

import com.ecommerce.dto.DashboardResponseDTO;

public interface DashboardService {
    DashboardResponseDTO getDashboardData(String bearerToken);
}
