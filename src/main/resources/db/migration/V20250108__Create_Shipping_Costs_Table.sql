-- Create shipping_costs table
CREATE TABLE shipping_costs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    distance_km_cost DECIMAL(10,4),
    weight_kg_cost DECIMAL(10,4),
    base_fee DECIMAL(10,2),
    international_fee DECIMAL(10,2),
    max_weight_kg DECIMAL(8,2),
    max_distance_km DECIMAL(8,2),
    free_shipping_threshold DECIMAL(10,2),
    region_code VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority_order INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_shipping_costs_region_active ON shipping_costs(region_code, is_active);
CREATE INDEX idx_shipping_costs_priority ON shipping_costs(priority_order);
CREATE INDEX idx_shipping_costs_name ON shipping_costs(name);

-- Insert some sample shipping cost configurations
INSERT INTO shipping_costs (name, description, distance_km_cost, weight_kg_cost, base_fee, international_fee, max_weight_kg, max_distance_km, free_shipping_threshold, region_code, is_active, priority_order) VALUES
('US Standard Shipping', 'Standard shipping within the United States', 0.50, 2.00, 5.99, 15.99, 50.00, 5000.00, 75.00, 'US', TRUE, 1),
('US Express Shipping', 'Express shipping within the United States', 0.75, 3.00, 12.99, 25.99, 30.00, 3000.00, 100.00, 'US', TRUE, 2),
('CA Standard Shipping', 'Standard shipping within Canada', 0.60, 2.50, 7.99, 18.99, 40.00, 4000.00, 100.00, 'CA', TRUE, 1),
('International Standard', 'Standard international shipping', 1.00, 4.00, 15.99, 0.00, 20.00, 20000.00, 200.00, 'INTL', TRUE, 1),
('Local Delivery', 'Local delivery within 50km', 0.25, 1.50, 2.99, 0.00, 25.00, 50.00, 50.00, 'LOCAL', TRUE, 1);
