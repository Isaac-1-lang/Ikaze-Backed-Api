package com.ecommerce.repository;

import com.ecommerce.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    /**
     * Find warehouse by name
     */
    Optional<Warehouse> findByName(String name);
    
    /**
     * Find all active warehouses
     */
    List<Warehouse> findByIsActiveTrue();
    
    /**
     * Find warehouse by location
     */
    List<Warehouse> findByLocationContainingIgnoreCase(String location);
    
    /**
     * Check if warehouse exists by name
     */
    boolean existsByName(String name);
}
