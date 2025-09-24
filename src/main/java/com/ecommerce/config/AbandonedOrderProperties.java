package com.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.abandoned-order-cleanup")
@Data
public class AbandonedOrderProperties {

    private int expiryMinutes = 30;
    private int batchSize = 50;
    private String cleanupSchedule = "0 0/10 * * * *";
    private boolean enabled = true;
    private boolean detailedLogging = true;
    private int maxRetryAttempts = 3;
    private boolean notifyOnFailure = false;
    private String failureNotificationEmail;
    private boolean dryRun = false;
}
