-- Create order_activity_logs table for tracking all order events
CREATE TABLE IF NOT EXISTS order_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    timestamp DATETIME NOT NULL,
    actor_type VARCHAR(50),
    actor_id VARCHAR(100),
    actor_name VARCHAR(200),
    metadata TEXT,
    reference_id VARCHAR(100),
    reference_type VARCHAR(50),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_order_id (order_id),
    INDEX idx_activity_type (activity_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_reference (reference_id, reference_type),
    
    CONSTRAINT fk_order_activity_logs_order 
        FOREIGN KEY (order_id) 
        REFERENCES orders(order_id) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment to table
ALTER TABLE order_activity_logs COMMENT = 'Stores all activities and events that happen to orders for timeline/audit trail';
