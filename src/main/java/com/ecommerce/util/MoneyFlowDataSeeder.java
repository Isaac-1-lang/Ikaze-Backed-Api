package com.ecommerce.util;

import com.ecommerce.entity.MoneyFlow;
import com.ecommerce.enums.MoneyFlowType;
import com.ecommerce.repository.MoneyFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds the database with sample money flow data
 * This will only run if the money_flow table is empty
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoneyFlowDataSeeder implements CommandLineRunner {

    private final MoneyFlowRepository moneyFlowRepository;

    @Override
    public void run(String... args) {
        // Only seed if table is empty
        if (moneyFlowRepository.count() > 0) {
            log.info("Money flow data already exists. Skipping seed.");
            return;
        }

        log.info("Starting money flow data seeding...");

        List<MoneyFlow> moneyFlows = new ArrayList<>();
        BigDecimal currentBalance = BigDecimal.ZERO;
        Random random = new Random();

        // Start from 60 days ago
        LocalDateTime startDate = LocalDateTime.now().minusDays(60);

        // Seed data with realistic scenarios
        String[][] transactions = {
                // INFLOWS (Income)
                {"IN", "Customer order payment - Order #1001", "1250.00"},
                {"IN", "Customer order payment - Order #1002", "890.50"},
                {"IN", "Customer order payment - Order #1003", "2340.75"},
                {"IN", "Customer order payment - Order #1004", "567.25"},
                {"IN", "Customer order payment - Order #1005", "1890.00"},
                {"IN", "Customer order payment - Order #1006", "3450.50"},
                {"IN", "Customer order payment - Order #1007", "678.90"},
                {"IN", "Customer order payment - Order #1008", "2100.00"},
                {"IN", "Customer order payment - Order #1009", "1567.80"},
                {"IN", "Customer order payment - Order #1010", "4230.25"},
                {"IN", "Bulk order payment - Corporate client", "15000.00"},
                {"IN", "Customer order payment - Order #1011", "890.00"},
                {"IN", "Customer order payment - Order #1012", "1234.50"},
                {"IN", "Customer order payment - Order #1013", "2890.75"},
                {"IN", "Customer order payment - Order #1014", "567.00"},
                
                // OUTFLOWS (Expenses)
                {"OUT", "Product return refund - Order #1002", "890.50"},
                {"OUT", "Supplier payment - Electronics inventory", "5000.00"},
                {"OUT", "Product return refund - Order #1004", "567.25"},
                {"OUT", "Shipping costs - Courier service", "450.00"},
                {"OUT", "Product return refund - Damaged item", "1234.50"},
                {"OUT", "Supplier payment - Clothing inventory", "3500.00"},
                {"OUT", "Marketing expenses - Social media ads", "800.00"},
                {"OUT", "Product return refund - Wrong size", "678.90"},
                {"OUT", "Warehouse rent payment", "2500.00"},
                {"OUT", "Product return refund - Customer dissatisfaction", "567.00"},
                {"OUT", "Supplier payment - Accessories stock", "2200.00"},
                {"OUT", "Utility bills - Electricity and water", "350.00"},
                {"OUT", "Product return refund - Defective product", "890.00"},
                {"OUT", "Employee salaries payment", "8000.00"},
                {"OUT", "Product return refund - Late delivery compensation", "1567.80"},
        };

        // Create transactions with varying timestamps
        for (int i = 0; i < transactions.length; i++) {
            String[] txn = transactions[i];
            MoneyFlowType type = MoneyFlowType.valueOf(txn[0]);
            String description = txn[1];
            BigDecimal amount = new BigDecimal(txn[2]);

            // Calculate new balance
            if (type == MoneyFlowType.IN) {
                currentBalance = currentBalance.add(amount);
            } else {
                currentBalance = currentBalance.subtract(amount);
            }

            // Create money flow with timestamp spread over 60 days
            MoneyFlow moneyFlow = new MoneyFlow();
            moneyFlow.setDescription(description);
            moneyFlow.setType(type);
            moneyFlow.setAmount(amount);
            moneyFlow.setRemainingBalance(currentBalance);
            
            // Set created_at manually for seeding (spread transactions over time)
            // Add some randomness to make it more realistic
            int daysOffset = (int) (i * (60.0 / transactions.length));
            int hoursOffset = random.nextInt(24);
            int minutesOffset = random.nextInt(60);
            LocalDateTime createdAt = startDate.plusDays(daysOffset)
                    .plusHours(hoursOffset)
                    .plusMinutes(minutesOffset);
            
            moneyFlow.setCreatedAt(createdAt);

            moneyFlows.add(moneyFlow);
        }

        // Sort by created date before saving to maintain chronological order
        moneyFlows.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        // Recalculate balances in chronological order
        currentBalance = BigDecimal.ZERO;
        for (MoneyFlow mf : moneyFlows) {
            if (mf.getType() == MoneyFlowType.IN) {
                currentBalance = currentBalance.add(mf.getAmount());
            } else {
                currentBalance = currentBalance.subtract(mf.getAmount());
            }
            mf.setRemainingBalance(currentBalance);
        }

        // Save all money flows
        moneyFlowRepository.saveAll(moneyFlows);

        log.info("Successfully seeded {} money flow transactions", moneyFlows.size());
        log.info("Final balance: {}", currentBalance);
    }
}
