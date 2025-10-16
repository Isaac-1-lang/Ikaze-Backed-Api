package com.ecommerce.service;

import com.ecommerce.dto.CreateMoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowResponseDTO;
import com.ecommerce.entity.MoneyFlow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MoneyFlowService {

    /**
     * Create a new money flow transaction
     * @param createMoneyFlowDTO The money flow data
     * @return The created money flow
     */
    MoneyFlow save(CreateMoneyFlowDTO createMoneyFlowDTO);

    /**
     * Get money flow data with automatic granularity determination
     * @param start Start date/time
     * @param end End date/time
     * @return Aggregated money flow data
     */
    MoneyFlowResponseDTO getMoneyFlow(LocalDateTime start, LocalDateTime end);

    /**
     * Get all money flow transactions within a date range
     * @param start Start date/time
     * @param end End date/time
     * @return List of money flow DTOs
     */
    List<MoneyFlowDTO> getTransactions(LocalDateTime start, LocalDateTime end);

    /**
     * Get the current account balance
     * @return Current balance
     */
    BigDecimal getCurrentBalance();

    /**
     * Get a specific money flow by ID
     * @param id The money flow ID
     * @return The money flow DTO
     */
    MoneyFlowDTO getById(Long id);

    /**
     * Delete a money flow transaction
     * @param id The money flow ID
     */
    void delete(Long id);
    
    /**
     * Get net revenue (total inflow minus total outflow)
     * This represents the actual revenue after refunds and other outflows
     * @return Net revenue amount
     */
    BigDecimal getNetRevenue();
}
