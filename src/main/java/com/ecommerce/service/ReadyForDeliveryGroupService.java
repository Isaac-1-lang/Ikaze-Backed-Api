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

    Page<ReadyForDeliveryGroupDTO> getAllGroups(Pageable pageable);

    List<ReadyForDeliveryGroupDTO> getAllGroups();

    // New methods for enhanced workflow
    Page<DeliveryGroupDto> listAvailableGroups(Pageable pageable);

    DeliveryGroupDto createGroupEnhanced(CreateReadyForDeliveryGroupDTO request);

    BulkAddResult addOrdersToGroupBulk(Long groupId, List<Long> orderIds);

    void removeOrderFromGroup(Long groupId, Long orderId);

    Page<AgentDto> listAvailableAgents(Pageable pageable, Sort sort);

    Optional<DeliveryGroupDto> findGroupByOrder(Long orderId);

    DeliveryAgentDashboardDTO getDeliveryAgentDashboard(UUID agentId);

    List<OrderDTO> getOrdersForGroup(Long groupId, UUID agentId);

    DeliveryGroupDto startDelivery(Long groupId, UUID agentId);

    DeliveryGroupDto finishDelivery(Long groupId, UUID agentId);

    OrderDTO getOrderDetailsForAgent(Long orderId, UUID agentId);

    // New delivery start/finish methods
    Map<String, Object> startDelivery(Long groupId);

    Map<String, Object> finishDelivery(Long groupId);
}
