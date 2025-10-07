package com.ecommerce.repository;

import com.ecommerce.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
         * Find warehouse by address
         */
        List<Warehouse> findByAddressContainingIgnoreCase(String address);

        /**
         * Find warehouse by city
         */
        List<Warehouse> findByCityContainingIgnoreCase(String city);

        /**
         * Check if warehouse exists by name
         */
        boolean existsByName(String name);

        /**
         * Find warehouses by address (searches in the address field)
         */
        @Query("SELECT w FROM Warehouse w WHERE w.address LIKE %:address%")
        List<Warehouse> findByAddressContaining(@Param("address") String address);

        /**
         * Find warehouses near a specific location using Haversine formula
         */
        @Query("SELECT w FROM Warehouse w WHERE " +
                        "6371 * acos(cos(radians(:latitude)) * cos(radians(w.latitude)) * " +
                        "cos(radians(w.longitude) - radians(:longitude)) + " +
                        "sin(radians(:latitude)) * sin(radians(w.latitude))) <= :radiusKm")
        List<Warehouse> findWarehousesNearLocation(@Param("latitude") Double latitude,
                        @Param("longitude") Double longitude,
                        @Param("radiusKm") Double radiusKm);

        /**
         * Check if warehouse exists by country (case insensitive)
         */
        @Query("SELECT COUNT(w) > 0 FROM Warehouse w WHERE LOWER(w.country) = LOWER(:country)")
        boolean existsByCountryIgnoreCase(@Param("country") String country);

        /**
         * Get all unique countries from warehouses
         */
        @Query("SELECT DISTINCT w.country FROM Warehouse w WHERE w.country IS NOT NULL AND w.country != '' ORDER BY w.country")
        List<String> findDistinctCountries();

        /**
         * Find active warehouses by country (case insensitive)
         */
        List<Warehouse> findByCountryIgnoreCaseAndIsActiveTrue(String country);
}
