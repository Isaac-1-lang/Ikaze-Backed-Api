package com.ecommerce.repository;

import com.ecommerce.entity.ReadyForDeliveryGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadyForDeliveryGroupRepository extends JpaRepository<ReadyForDeliveryGroup, Long> {

        @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.shopOrders WHERE g.deliveryGroupId = :id")
        Optional<ReadyForDeliveryGroup> findByIdWithOrders(@Param("id") Long id);

        @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.deliverer WHERE g.deliveryGroupId = :id")
        Optional<ReadyForDeliveryGroup> findByIdWithDeliverer(@Param("id") Long id);

        @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.shopOrders LEFT JOIN FETCH g.deliverer")
        List<ReadyForDeliveryGroup> findAllWithOrdersAndDeliverer();

        @EntityGraph(attributePaths = { "shopOrders", "deliverer" })
        @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.hasDeliveryStarted = false")
        Page<ReadyForDeliveryGroup> findAllWithOrdersAndDeliverer(Pageable pageable);

        @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.shopOrders LEFT JOIN FETCH g.deliverer WHERE g.deliveryGroupId = :id")
        Optional<ReadyForDeliveryGroup> findByIdWithOrdersAndDeliverer(@Param("id") Long id);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.readyForDeliveryGroup.deliveryGroupId = :groupId")
        Long countOrdersInGroup(@Param("groupId") Long groupId);

        @Query("SELECT so.id FROM ShopOrder so WHERE so.readyForDeliveryGroup.deliveryGroupId = :groupId")
        List<Long> findOrderIdsInGroup(@Param("groupId") Long groupId);

        @Query("SELECT COUNT(g) FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId")
        Long countByDelivererId(@Param("delivererId") java.util.UUID delivererId);

        @Query("SELECT COUNT(so) FROM ShopOrder so WHERE so.readyForDeliveryGroup.deliverer.id = :delivererId")
        Long countOrdersByDelivererId(@Param("delivererId") java.util.UUID delivererId);

        /**
         * Count active delivery groups for a deliverer (groups that are not completed)
         * Active means: delivery has started OR not started yet, but NOT finished
         */
        @Query("SELECT COUNT(g) FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId AND g.hasDeliveryFinished = false")
        Long countActiveGroupsByDelivererId(@Param("delivererId") java.util.UUID delivererId);

        /**
         * Find delivery groups by deliverer ID where delivery is not finished
         */
        @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId AND g.hasDeliveryFinished = false")
        List<ReadyForDeliveryGroup> findByDelivererIdAndHasDeliveryFinishedFalse(
                        @Param("delivererId") java.util.UUID delivererId);

        /**
         * Find delivery groups by deliverer ID where delivery is finished
         */
        @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId AND g.hasDeliveryFinished = true")
        List<ReadyForDeliveryGroup> findByDelivererIdAndHasDeliveryFinishedTrue(
                        @Param("delivererId") java.util.UUID delivererId);

        /**
         * Search delivery groups by name, description, or deliverer name
         */
        @EntityGraph(attributePaths = { "shopOrders", "deliverer" })
        @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.hasDeliveryStarted = false AND " +
                        "(LOWER(g.deliveryGroupName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(g.deliveryGroupDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(CONCAT(g.deliverer.firstName, ' ', g.deliverer.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<ReadyForDeliveryGroup> searchAvailableGroups(@Param("search") String search, Pageable pageable);

        /**
         * Find ALL delivery groups with orders and deliverer - NO EXCLUSIONS
         * This includes groups that have started, finished, or not started yet
         */
        @EntityGraph(attributePaths = { "shopOrders", "deliverer" })
        @Query("SELECT g FROM ReadyForDeliveryGroup g")
        Page<ReadyForDeliveryGroup> findAllGroupsWithoutExclusions(Pageable pageable);

        /**
         * Search ALL delivery groups by name, description, or deliverer name - NO
         * EXCLUSIONS
         * This includes groups that have started, finished, or not started yet
         */
        @EntityGraph(attributePaths = { "shopOrders", "deliverer" })
        @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE " +
                        "LOWER(g.deliveryGroupName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(g.deliveryGroupDescription) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(CONCAT(g.deliverer.firstName, ' ', g.deliverer.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<ReadyForDeliveryGroup> searchAllGroupsWithoutExclusions(@Param("search") String search, Pageable pageable);
}
