package com.ecommerce.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaFix {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void fixOrderAddressSchema() {
        try {
            log.info("Applying database schema fix for order_addresses.zipcode column");
            jdbcTemplate.execute("ALTER TABLE order_addresses ALTER COLUMN zipcode DROP NOT NULL");
            log.info("Successfully made zipcode column nullable in order_addresses table");
        } catch (Exception e) {
            log.warn("Schema fix for zipcode column failed (might already be applied): {}", e.getMessage());
        }
    }
}
