package com.ecommerce.repository;

import com.ecommerce.entity.DeliveryArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryAreaRepository extends JpaRepository<DeliveryArea, Long> {
    
    /**
     * Find all top-level delivery areas (areas with no parent)
     * 
     * @return list of top-level delivery areas
     */
    List<DeliveryArea> findByParentIsNull();
    
    /**
     * Find all sub-areas of a given parent delivery area
     * 
     * @param parentId the ID of the parent delivery area
     * @return list of child delivery areas
     */
    List<DeliveryArea> findByParentDeliveryAreaId(Long parentId);
    
    /**
     * Find a delivery area by its name
     * 
     * @param name the name of the delivery area
     * @return the delivery area if found
     */
    DeliveryArea findByDeliveryAreaName(String name);
    
    /**
     * Check if a delivery area has any children
     * 
     * @param deliveryAreaId the ID of the delivery area
     * @return true if the delivery area has children, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DeliveryArea d WHERE d.parent.deliveryAreaId = ?1")
    boolean hasChildren(Long deliveryAreaId);
}