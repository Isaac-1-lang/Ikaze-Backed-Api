package com.ecommerce.repository;

import com.ecommerce.entity.ReadyForDeliveryGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReadyForDeliveryGroupRepository extends JpaRepository<ReadyForDeliveryGroup, Long> {

    @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.orders WHERE g.deliveryGroupId = :id")
    Optional<ReadyForDeliveryGroup> findByIdWithOrders(@Param("id") Long id);

    @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.deliverer WHERE g.deliveryGroupId = :id")
    Optional<ReadyForDeliveryGroup> findByIdWithDeliverer(@Param("id") Long id);

    @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.orders LEFT JOIN FETCH g.deliverer")
    List<ReadyForDeliveryGroup> findAllWithOrdersAndDeliverer();

    @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.orders LEFT JOIN FETCH g.deliverer")
    Page<ReadyForDeliveryGroup> findAllWithOrdersAndDeliverer(Pageable pageable);

    @Query("SELECT g FROM ReadyForDeliveryGroup g LEFT JOIN FETCH g.orders LEFT JOIN FETCH g.deliverer WHERE g.deliveryGroupId = :id")
    Optional<ReadyForDeliveryGroup> findByIdWithOrdersAndDeliverer(@Param("id") Long id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.readyForDeliveryGroup.deliveryGroupId = :groupId")
    Long countOrdersInGroup(@Param("groupId") Long groupId);

    @Query("SELECT o.orderId FROM Order o WHERE o.readyForDeliveryGroup.deliveryGroupId = :groupId")
    List<java.util.UUID> findOrderIdsInGroup(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(g) FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId")
    Long countByDelivererId(@Param("delivererId") java.util.UUID delivererId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.readyForDeliveryGroup.deliverer.id = :delivererId")
    Long countOrdersByDelivererId(@Param("delivererId") java.util.UUID delivererId);

    /**
     * Find delivery groups by deliverer ID where delivery is not finished
     */
    @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId AND g.hasDeliveryFinished = false")
    List<ReadyForDeliveryGroup> findByDelivererIdAndHasDeliveryFinishedFalse(@Param("delivererId") java.util.UUID delivererId);

    /**
     * Find delivery groups by deliverer ID where delivery is finished
     */
    @Query("SELECT g FROM ReadyForDeliveryGroup g WHERE g.deliverer.id = :delivererId AND g.hasDeliveryFinished = true")
    List<ReadyForDeliveryGroup> findByDelivererIdAndHasDeliveryFinishedTrue(@Param("delivererId") java.util.UUID delivererId);
}
