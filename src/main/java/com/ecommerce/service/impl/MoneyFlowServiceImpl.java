package com.ecommerce.service.impl;

import com.ecommerce.dto.CreateMoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowAggregationDTO;
import com.ecommerce.dto.MoneyFlowDTO;
import com.ecommerce.dto.MoneyFlowResponseDTO;
import com.ecommerce.entity.MoneyFlow;
import com.ecommerce.enums.MoneyFlowType;
import com.ecommerce.repository.MoneyFlowRepository;
import com.ecommerce.service.MoneyFlowService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoneyFlowServiceImpl implements MoneyFlowService {

    private final MoneyFlowRepository moneyFlowRepository;

    @Override
    public MoneyFlow save(CreateMoneyFlowDTO createMoneyFlowDTO) {
        log.info("Creating new money flow: type={}, amount={}", 
                createMoneyFlowDTO.getType(), createMoneyFlowDTO.getAmount());

        BigDecimal currentBalance = getCurrentBalance();

        BigDecimal newBalance;
        if (createMoneyFlowDTO.getType() == MoneyFlowType.IN) {
            newBalance = currentBalance.add(createMoneyFlowDTO.getAmount());
        } else {
            newBalance = currentBalance.subtract(createMoneyFlowDTO.getAmount());
        }

        // Create money flow entity
        MoneyFlow moneyFlow = new MoneyFlow();
        moneyFlow.setDescription(createMoneyFlowDTO.getDescription());
        moneyFlow.setType(createMoneyFlowDTO.getType());
        moneyFlow.setAmount(createMoneyFlowDTO.getAmount());
        moneyFlow.setRemainingBalance(newBalance);

        MoneyFlow saved = moneyFlowRepository.save(moneyFlow);
        log.info("Money flow created successfully with ID: {}, new balance: {}", saved.getId(), newBalance);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public MoneyFlowResponseDTO getMoneyFlow(LocalDateTime start, LocalDateTime end) {
        log.info("Fetching money flow data from {} to {}", start, end);

        // Determine granularity based on time range
        Duration duration = Duration.between(start, end);
        String granularity = determineGranularity(duration);

        log.info("Determined granularity: {} for duration: {} hours", granularity, duration.toHours());

        List<MoneyFlowAggregationDTO> aggregations;

        switch (granularity) {
            case "minute":
                aggregations = aggregateByMinute(start, end);
                break;
            case "hour":
                aggregations = aggregateByHour(start, end);
                break;
            case "day":
                aggregations = aggregateByDay(start, end);
                break;
            case "week":
                aggregations = aggregateByWeek(start, end);
                break;
            case "month":
                aggregations = aggregateByMonth(start, end);
                break;
            case "year":
                aggregations = aggregateByYear(start, end);
                break;
            default:
                throw new IllegalArgumentException("Invalid granularity: " + granularity);
        }

        return MoneyFlowResponseDTO.builder()
                .startDate(start)
                .endDate(end)
                .granularity(granularity)
                .aggregations(aggregations)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MoneyFlowDTO> getTransactions(LocalDateTime start, LocalDateTime end) {
        log.info("Fetching transactions from {} to {}", start, end);
        
        List<MoneyFlow> transactions = moneyFlowRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
        return transactions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance() {
        return moneyFlowRepository.findTopByOrderByCreatedAtDesc()
                .map(MoneyFlow::getRemainingBalance)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public MoneyFlowDTO getById(Long id) {
        log.info("Fetching money flow with ID: {}", id);
        
        MoneyFlow moneyFlow = moneyFlowRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Money flow not found with ID: " + id));
        
        return mapToDTO(moneyFlow);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting money flow with ID: {}", id);
        
        if (!moneyFlowRepository.existsById(id)) {
            throw new EntityNotFoundException("Money flow not found with ID: " + id);
        }
        
        moneyFlowRepository.deleteById(id);
        log.info("Money flow deleted successfully with ID: {}", id);
    }

    /**
     * Determine granularity based on time range duration
     */
    private String determineGranularity(Duration duration) {
        long hours = duration.toHours();
        long days = duration.toDays();

        if (hours <= 1) {
            return "minute";
        } else if (hours <= 24) {
            return "hour";
        } else if (days <= 30) {
            return "day";
        } else if (days <= 180) {
            return "week";
        } else if (days <= 365) {
            return "month";
        } else {
            return "year";
        }
    }

    /**
     * Aggregate by minute with detailed transactions
     */
    private List<MoneyFlowAggregationDTO> aggregateByMinute(LocalDateTime start, LocalDateTime end) {
        List<Object[]> aggregateResults = moneyFlowRepository.aggregateByMinute(start, end);
        List<Object[]> detailedResults = moneyFlowRepository.getDetailedTransactionsByMinute(start, end);

        // Group detailed transactions by period
        Map<String, List<MoneyFlowDTO>> transactionsByPeriod = new HashMap<>();
        for (Object[] row : detailedResults) {
            String period = (String) row[0];
            MoneyFlowDTO dto = mapDetailedRowToDTO(row);
            transactionsByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(dto);
        }

        // Build aggregations with transactions
        List<MoneyFlowAggregationDTO> aggregations = new ArrayList<>();
        for (Object[] row : aggregateResults) {
            String period = (String) row[0];
            BigDecimal totalInflow = (BigDecimal) row[1];
            BigDecimal totalOutflow = (BigDecimal) row[2];
            BigDecimal netBalance = totalInflow.subtract(totalOutflow);

            aggregations.add(MoneyFlowAggregationDTO.builder()
                    .period(period)
                    .totalInflow(totalInflow)
                    .totalOutflow(totalOutflow)
                    .netBalance(netBalance)
                    .transactions(transactionsByPeriod.getOrDefault(period, new ArrayList<>()))
                    .build());
        }

        return aggregations;
    }

    /**
     * Aggregate by hour with detailed transactions
     */
    private List<MoneyFlowAggregationDTO> aggregateByHour(LocalDateTime start, LocalDateTime end) {
        List<Object[]> aggregateResults = moneyFlowRepository.aggregateByHour(start, end);
        List<Object[]> detailedResults = moneyFlowRepository.getDetailedTransactionsByHour(start, end);

        // Group detailed transactions by period
        Map<String, List<MoneyFlowDTO>> transactionsByPeriod = new HashMap<>();
        for (Object[] row : detailedResults) {
            String period = (String) row[0];
            MoneyFlowDTO dto = mapDetailedRowToDTO(row);
            transactionsByPeriod.computeIfAbsent(period, k -> new ArrayList<>()).add(dto);
        }

        // Build aggregations with transactions
        List<MoneyFlowAggregationDTO> aggregations = new ArrayList<>();
        for (Object[] row : aggregateResults) {
            String period = (String) row[0];
            BigDecimal totalInflow = (BigDecimal) row[1];
            BigDecimal totalOutflow = (BigDecimal) row[2];
            BigDecimal netBalance = totalInflow.subtract(totalOutflow);

            aggregations.add(MoneyFlowAggregationDTO.builder()
                    .period(period)
                    .totalInflow(totalInflow)
                    .totalOutflow(totalOutflow)
                    .netBalance(netBalance)
                    .transactions(transactionsByPeriod.getOrDefault(period, new ArrayList<>()))
                    .build());
        }

        return aggregations;
    }

    /**
     * Aggregate by day (no detailed transactions)
     */
    private List<MoneyFlowAggregationDTO> aggregateByDay(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = moneyFlowRepository.aggregateByDay(start, end);
        return buildAggregations(results);
    }

    /**
     * Aggregate by week (no detailed transactions)
     */
    private List<MoneyFlowAggregationDTO> aggregateByWeek(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = moneyFlowRepository.aggregateByWeek(start, end);
        return buildAggregations(results);
    }

    /**
     * Aggregate by month (no detailed transactions)
     */
    private List<MoneyFlowAggregationDTO> aggregateByMonth(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = moneyFlowRepository.aggregateByMonth(start, end);
        return buildAggregations(results);
    }

    /**
     * Aggregate by year (no detailed transactions)
     */
    private List<MoneyFlowAggregationDTO> aggregateByYear(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = moneyFlowRepository.aggregateByYear(start, end);
        return buildAggregations(results);
    }

    /**
     * Build aggregations from query results (without detailed transactions)
     */
    private List<MoneyFlowAggregationDTO> buildAggregations(List<Object[]> results) {
        List<MoneyFlowAggregationDTO> aggregations = new ArrayList<>();
        
        for (Object[] row : results) {
            String period = (String) row[0];
            BigDecimal totalInflow = (BigDecimal) row[1];
            BigDecimal totalOutflow = (BigDecimal) row[2];
            BigDecimal netBalance = totalInflow.subtract(totalOutflow);

            aggregations.add(MoneyFlowAggregationDTO.builder()
                    .period(period)
                    .totalInflow(totalInflow)
                    .totalOutflow(totalOutflow)
                    .netBalance(netBalance)
                    .transactions(null) // No detailed transactions for larger granularities
                    .build());
        }

        return aggregations;
    }

    /**
     * Map detailed query row to MoneyFlowDTO
     */
    private MoneyFlowDTO mapDetailedRowToDTO(Object[] row) {
        // row format: period, id, description, type, amount, remaining_balance, created_at
        Long id = ((Number) row[1]).longValue();
        String description = (String) row[2];
        String typeStr = (String) row[3];
        BigDecimal amount = (BigDecimal) row[4];
        BigDecimal remainingBalance = (BigDecimal) row[5];
        LocalDateTime createdAt = ((Timestamp) row[6]).toLocalDateTime();

        return MoneyFlowDTO.builder()
                .id(id)
                .description(description)
                .type(MoneyFlowType.valueOf(typeStr))
                .amount(amount)
                .remainingBalance(remainingBalance)
                .createdAt(createdAt)
                .build();
    }

    /**
     * Map MoneyFlow entity to DTO
     */
    private MoneyFlowDTO mapToDTO(MoneyFlow moneyFlow) {
        return MoneyFlowDTO.builder()
                .id(moneyFlow.getId())
                .description(moneyFlow.getDescription())
                .type(moneyFlow.getType())
                .amount(moneyFlow.getAmount())
                .remainingBalance(moneyFlow.getRemainingBalance())
                .createdAt(moneyFlow.getCreatedAt())
                .build();
    }
}
