package com.ecommerce.repository;

import com.ecommerce.entity.OrderDeliveryNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderDeliveryNoteRepository extends JpaRepository<OrderDeliveryNote, Long> {

    /**
     * Find all notes for a specific order (not deleted)
     */
    @Query("SELECT n FROM OrderDeliveryNote n WHERE n.shopOrder.order.orderId = :orderId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<OrderDeliveryNote> findByOrderId(@Param("orderId") Long orderId, Pageable pageable);

    /**
     * Find all notes for a specific delivery group (not deleted)
     */
    @Query("SELECT n FROM OrderDeliveryNote n WHERE n.deliveryGroup.deliveryGroupId = :groupId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<OrderDeliveryNote> findByDeliveryGroupId(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * Find all notes by a specific agent (not deleted)
     */
    @Query("SELECT n FROM OrderDeliveryNote n WHERE n.agent.id = :agentId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<OrderDeliveryNote> findByAgentId(@Param("agentId") String agentId, Pageable pageable);

    /**
     * Find note by ID (not deleted)
     */
    @Query("SELECT n FROM OrderDeliveryNote n WHERE n.noteId = :noteId AND n.isDeleted = false")
    Optional<OrderDeliveryNote> findByIdNotDeleted(@Param("noteId") Long noteId);

    /**
     * Find all notes for orders in a delivery group (not deleted)
     */
    @Query("SELECT n FROM OrderDeliveryNote n WHERE n.shopOrder.readyForDeliveryGroup.deliveryGroupId = :groupId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    Page<OrderDeliveryNote> findAllNotesForDeliveryGroup(@Param("groupId") Long groupId, Pageable pageable);

    /**
     * Count notes for an order
     */
    @Query("SELECT COUNT(n) FROM OrderDeliveryNote n WHERE n.shopOrder.order.orderId = :orderId AND n.isDeleted = false")
    long countByOrderId(@Param("orderId") Long orderId);

    /**
     * Count notes for a delivery group
     */
    @Query("SELECT COUNT(n) FROM OrderDeliveryNote n WHERE n.deliveryGroup.deliveryGroupId = :groupId AND n.isDeleted = false")
    long countByDeliveryGroupId(@Param("groupId") Long groupId);
}
