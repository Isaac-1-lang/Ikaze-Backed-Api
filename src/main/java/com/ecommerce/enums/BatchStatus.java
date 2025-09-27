package com.ecommerce.enums;

/**
 * Enum representing the lifecycle status of a stock batch
 */
public enum BatchStatus {
    /**
     * Batch is active and available for use
     */
    ACTIVE,

    /**
     * Batch has been completely consumed/used up
     */
    EMPTY,

    /**
     * Batch has expired and should not be used
     */
    EXPIRED,

    /**
     * Batch has been recalled due to quality issues or safety concerns
     */
    RECALLED,

    /**
     * Batch has been deactivated (typically when reassigning stock but preserving for order history)
     */
    INACTIVE
}
