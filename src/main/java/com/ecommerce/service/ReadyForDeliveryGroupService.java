package com.ecommerce.service;

import com.ecommerce.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReadyForDeliveryGroupService {
    ReadyForDeliveryGroupDTO createGroup(CreateReadyForDeliveryGroupDTO request);

    ReadyForDeliveryGroupDTO addOrdersToGroup(Long groupId, AddOrdersToGroupDTO request);

    void removeOrdersFromGroup(Long groupId, List<Long> orderIds);

    ReadyForDeliveryGroupDTO updateGroup(Long groupId, UpdateReadyForDeliveryGroupDTO request);

    void deleteGroup(Long groupId);

    ReadyForDeliveryGroupDTO markDeliveryStarted(Long groupId);

    ReadyForDeliveryGroupDTO getGroupById(Long groupId);

    Page<ReadyForDeliveryGroupDTO> getAllGroups(java.util.UUID shopId, Pageable pageable);

    List<ReadyForDeliveryGroupDTO> getAllGroups(java.util.UUID shopId);

    // New methods for enhanced workflow
    Page<DeliveryGroupDto> listAvailableGroups(java.util.UUID shopId, Pageable pageable);

    Page<DeliveryGroupDto> listAvailableGroups(java.util.UUID shopId, String search, Pageable pageable);

    DeliveryGroupDto createGroupEnhanced(CreateReadyForDeliveryGroupDTO request);

    BulkAddResult addOrdersToGroupBulk(Long groupId, List<Long> orderIds);

    void removeOrderFromGroup(Long groupId, Long orderId);

    Page<AgentDto> listAvailableAgents(java.util.UUID shopId, Pageable pageable, Sort sort);

    Page<AgentDto> listAvailableAgents(java.util.UUID shopId, String search, Pageable pageable, Sort sort);

    Optional<DeliveryGroupDto> findGroupByOrder(Long orderId);

    DeliveryAgentDashboardDTO getDeliveryAgentDashboard(UUID agentId);

    List<OrderDTO> getOrdersForGroup(Long groupId, UUID agentId);

    DeliveryGroupDto startDelivery(Long groupId, UUID agentId);

    DeliveryGroupDto finishDelivery(Long groupId, UUID agentId);

    OrderDTO getOrderDetailsForAgent(Long orderId, UUID agentId);

    // New delivery start/finish methods
    Map<String, Object> startDelivery(Long groupId);

    Map<String, Object> finishDelivery(Long groupId);

    // Change order's delivery group assignment
    DeliveryGroupDto changeOrderGroup(Long orderId, Long newGroupId);

    /**
     * Get ALL delivery groups without any exclusions (includes started, finished,
     * and pending groups)
     * Supports optional search by name, description, or deliverer name
     * 
     * @param search   Optional search term (can be null or empty)
     * @param pageable Pagination parameters
     * @return Page of all delivery groups
     */
    Page<ReadyForDeliveryGroupDTO> getAllGroupsWithoutExclusions(java.util.UUID shopId, String search,
            Pageable pageable);

    /**
     * Get orders for a specific delivery group with pagination
     * 
     * @param groupId  Delivery group ID
     * @param pageable Pagination parameters
     * @return Page of orders in the group
     */
    Page<OrderDTO> getOrdersForGroupWithPagination(Long groupId, Pageable pageable);
}
