package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.dto.*;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.ReadyForDeliveryGroup;
import com.ecommerce.entity.User;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ReadyForDeliveryGroupRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.ReadyForDeliveryGroupService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadyForDeliveryGroupServiceImpl implements ReadyForDeliveryGroupService {

    private final ReadyForDeliveryGroupRepository groupRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final com.ecommerce.service.OrderActivityLogService activityLogService;

    @Override
    @Transactional
    public ReadyForDeliveryGroupDTO createGroup(CreateReadyForDeliveryGroupDTO request) {
        log.info("Creating ready for delivery group: {}", request.getDeliveryGroupName());

        User deliverer = userRepository.findById(request.getDelivererId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Deliverer not found with ID: " + request.getDelivererId()));
        
        // Check if the deliverer already has 5 or more active groups
        Long activeGroupCount = groupRepository.countActiveGroupsByDelivererId(deliverer.getId());
        if (activeGroupCount >= 5) {
            throw new IllegalStateException(
                "Delivery agent already has " + activeGroupCount + " active delivery groups. Maximum allowed is 5.");
        }
        
        ReadyForDeliveryGroup group = new ReadyForDeliveryGroup();
        group.setDeliveryGroupName(request.getDeliveryGroupName());
        group.setDeliveryGroupDescription(request.getDeliveryGroupDescription());
        group.setDeliverer(deliverer);
        group.setHasDeliveryStarted(false);

        ReadyForDeliveryGroup savedGroup = groupRepository.save(group);

        if (request.getOrderIds() != null && !request.getOrderIds().isEmpty()) {
            addOrdersToGroupInternal(savedGroup.getDeliveryGroupId(), request.getOrderIds());
        }

        log.info("Created ready for delivery group with ID: {}", savedGroup.getDeliveryGroupId());
        return mapToDTO(savedGroup);
    }

    @Override
    @Transactional
    public ReadyForDeliveryGroupDTO addOrdersToGroup(Long groupId, AddOrdersToGroupDTO request) {
        log.info("Adding orders to group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot add orders to a group that has already started delivery");
        }

        addOrdersToGroupInternal(groupId, request.getOrderIds());

        ReadyForDeliveryGroup updatedGroup = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        log.info("Added {} orders to group: {}", request.getOrderIds().size(), groupId);
        return mapToDTO(updatedGroup);
    }

    @Override
    @Transactional
    public void removeOrdersFromGroup(Long groupId, List<Long> orderIds) {
        log.info("Removing orders from group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot remove orders from a group that has already started delivery");
        }

        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

            if (order.getReadyForDeliveryGroup() != null &&
                    order.getReadyForDeliveryGroup().getDeliveryGroupId().equals(groupId)) {
                group.removeOrder(order);
            }
        }

        groupRepository.save(group);
        log.info("Removed {} orders from group: {}", orderIds.size(), groupId);
    }

    @Override
    @Transactional
    public ReadyForDeliveryGroupDTO updateGroup(Long groupId, UpdateReadyForDeliveryGroupDTO request) {
        log.info("Updating group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (request.getDeliveryGroupName() != null) {
            group.setDeliveryGroupName(request.getDeliveryGroupName());
        }
        if (request.getDeliveryGroupDescription() != null) {
            group.setDeliveryGroupDescription(request.getDeliveryGroupDescription());
        }
        if (request.getDelivererId() != null) {
            User deliverer = userRepository.findById(request.getDelivererId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Deliverer not found with ID: " + request.getDelivererId()));
            
            // Check if the new deliverer already has 5 or more active groups
            // Only check if we're actually changing the deliverer
            if (!deliverer.getId().equals(group.getDeliverer().getId())) {
                Long activeGroupCount = groupRepository.countActiveGroupsByDelivererId(deliverer.getId());
                if (activeGroupCount >= 5) {
                    throw new IllegalStateException(
                        "Delivery agent already has " + activeGroupCount + " active delivery groups. Maximum allowed is 5.");
                }
            }
            
            group.setDeliverer(deliverer);
        }

        ReadyForDeliveryGroup updatedGroup = groupRepository.save(group);
        log.info("Updated group: {}", groupId);
        return mapToDTO(updatedGroup);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId) {
        log.info("Deleting group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot delete a group that has already started delivery");
        }

        for (Order order : group.getOrders()) {
            order.setReadyForDeliveryGroup(null);
        }

        groupRepository.delete(group);
        log.info("Deleted group: {}", groupId);
    }

    @Override
    @Transactional
    public ReadyForDeliveryGroupDTO markDeliveryStarted(Long groupId) {
        log.info("Marking delivery as started for group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Delivery has already been started for this group");
        }

        group.setHasDeliveryStarted(true);
        group.setDeliveryStartedAt(LocalDateTime.now());

        ReadyForDeliveryGroup updatedGroup = groupRepository.save(group);
        log.info("Marked delivery as started for group: {}", groupId);
        return mapToDTO(updatedGroup);
    }

    @Override
    public ReadyForDeliveryGroupDTO getGroupById(Long groupId) {
        log.info("Getting group by ID: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrdersAndDeliverer(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        return mapToDTO(group);
    }

    @Override
    public Page<ReadyForDeliveryGroupDTO> getAllGroups(Pageable pageable) {
        log.info("Getting all groups with pagination: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());

        Page<ReadyForDeliveryGroup> groups = groupRepository.findAllWithOrdersAndDeliverer(pageable);
        return groups.map(this::mapToDTO);
    }

    @Override
    public List<ReadyForDeliveryGroupDTO> getAllGroups() {
        log.info("Getting all groups");

        List<ReadyForDeliveryGroup> groups = groupRepository.findAllWithOrdersAndDeliverer();
        return groups.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public Page<ReadyForDeliveryGroupDTO> getAllGroupsWithoutExclusions(String search, Pageable pageable) {
        log.info("Getting all groups without exclusions with pagination: page={}, size={}, search={}",
                pageable.getPageNumber(), pageable.getPageSize(), search);

        Page<ReadyForDeliveryGroup> groups;
        
        if (search != null && !search.trim().isEmpty()) {
            // Search across all groups (no exclusions)
            groups = groupRepository.searchAllGroupsWithoutExclusions(search.trim(), pageable);
            log.info("Found {} groups matching search term: '{}'", groups.getTotalElements(), search);
        } else {
            // Get all groups (no exclusions)
            groups = groupRepository.findAllGroupsWithoutExclusions(pageable);
            log.info("Found {} total groups", groups.getTotalElements());
        }

        return groups.map(this::mapToDTO);
    }

    @Override
    public Page<OrderDTO> getOrdersForGroupWithPagination(Long groupId, Pageable pageable) {
        log.info("Getting orders for group {} with pagination: page={}, size={}",
                groupId, pageable.getPageNumber(), pageable.getPageSize());

        // Verify group exists
        groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        // Get orders with pagination
        Page<Order> orders = orderRepository.findByReadyForDeliveryGroupId(groupId, pageable);
        log.info("Found {} orders for group {}", orders.getTotalElements(), groupId);

        return orders.map(this::mapToOrderDTO);
    }

    private void addOrdersToGroupInternal(Long groupId, List<Long> orderIds) {
        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        String deliveryAgentName = group.getDeliverer() != null
            ? group.getDeliverer().getFirstName() + " " + group.getDeliverer().getLastName()
            : "Unassigned";
        String deliveryAgentPhone = group.getDeliverer() != null && group.getDeliverer().getPhoneNumber() != null
            ? group.getDeliverer().getPhoneNumber()
            : "N/A";

        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

            if (order.getReadyForDeliveryGroup() != null) {
                throw new IllegalStateException("Order " + orderId + " is already assigned to another delivery group");
            }

            group.addOrder(order);

            // LOG ACTIVITY: Added to Delivery Group
            activityLogService.logAddedToDeliveryGroup(
                orderId,
                group.getDeliveryGroupName(),
                deliveryAgentName,
                deliveryAgentPhone,
                groupId
            );
        }

        groupRepository.save(group);
    }

    private ReadyForDeliveryGroupDTO mapToDTO(ReadyForDeliveryGroup group) {
        List<String> orderIds = group.getOrders().stream()
                .map(order -> order.getOrderId().toString())
                .collect(Collectors.toList());

        String delivererName = group.getDeliverer() != null
                ? group.getDeliverer().getFirstName() + " " + group.getDeliverer().getLastName()
                : null;

        int ordersCount = group.getOrders().size();
        
        return ReadyForDeliveryGroupDTO.builder()
                .deliveryGroupId(group.getDeliveryGroupId())
                .deliveryGroupName(group.getDeliveryGroupName())
                .deliveryGroupDescription(group.getDeliveryGroupDescription())
                .delivererId(group.getDeliverer() != null ? group.getDeliverer().getId() : null)
                .delivererName(delivererName)
                .orderIds(orderIds)
                .orderCount(ordersCount)
                .totalOrders(ordersCount) // Set totalOrders for frontend compatibility
                .createdAt(group.getCreatedAt())
                .scheduledAt(group.getScheduledAt())
                .hasDeliveryStarted(group.getHasDeliveryStarted())
                .deliveryStartedAt(group.getDeliveryStartedAt())
                .build();
    }

    @Override
    public Page<DeliveryGroupDto> listAvailableGroups(Pageable pageable) {
        log.info("Listing available groups with pagination: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());

        Page<ReadyForDeliveryGroup> groups = groupRepository.findAllWithOrdersAndDeliverer(pageable);
        return groups.map(this::mapToDeliveryGroupDto);
    }

    @Override
    public Page<DeliveryGroupDto> listAvailableGroups(String search, Pageable pageable) {
        log.info("Listing available groups with search='{}', page={}, size={}", 
                search, pageable.getPageNumber(), pageable.getPageSize());

        if (search == null || search.trim().isEmpty()) {
            return listAvailableGroups(pageable);
        }

        Page<ReadyForDeliveryGroup> groups = groupRepository.searchAvailableGroups(search.trim(), pageable);
        return groups.map(this::mapToDeliveryGroupDto);
    }

    @Override
    @Transactional
    public DeliveryGroupDto createGroupEnhanced(CreateReadyForDeliveryGroupDTO request) {
        log.info("Creating enhanced delivery group: {}", request.getDeliveryGroupName());

        ReadyForDeliveryGroupDTO group = createGroup(request);
        return mapToDeliveryGroupDto(groupRepository.findByIdWithOrdersAndDeliverer(group.getDeliveryGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found after creation")));
    }

    @Override
    @Transactional
    public BulkAddResult addOrdersToGroupBulk(Long groupId, List<Long> orderIds) {
        log.info("Bulk adding {} orders to group: {}", orderIds.size(), groupId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot add orders to a group that has already started delivery");
        }

        List<BulkAddResult.SkippedOrder> skippedOrders = new ArrayList<>();
        int successfullyAdded = 0;

        for (Long orderId : orderIds) {
            try {
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

                if (order.getReadyForDeliveryGroup() != null) {
                    skippedOrders.add(BulkAddResult.SkippedOrder.builder()
                            .orderId(orderId)
                            .reason("already_in_group")
                            .details("Order is already assigned to group: "
                                    + order.getReadyForDeliveryGroup().getDeliveryGroupName())
                            .build());
                } else {
                    group.addOrder(order);
                    successfullyAdded++;
                }
            } catch (Exception e) {
                skippedOrders.add(BulkAddResult.SkippedOrder.builder()
                        .orderId(orderId)
                        .reason("error")
                        .details(e.getMessage())
                        .build());
            }
        }

        if (successfullyAdded > 0) {
            groupRepository.save(group);
        }

        log.info("Bulk add completed: {} added, {} skipped", successfullyAdded, skippedOrders.size());

        return BulkAddResult.builder()
                .totalRequested(orderIds.size())
                .successfullyAdded(successfullyAdded)
                .skipped(skippedOrders.size())
                .skippedOrders(skippedOrders)
                .build();
    }

    @Override
    @Transactional
    public void removeOrderFromGroup(Long groupId, Long orderId) {
        log.info("Removing order {} from group: {}", orderId, groupId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot remove orders from a group that has already started delivery");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        if (order.getReadyForDeliveryGroup() != null &&
                order.getReadyForDeliveryGroup().getDeliveryGroupId().equals(groupId)) {
            group.removeOrder(order);
            groupRepository.save(group);
            log.info("Order {} removed from group {}", orderId, groupId);
        } else {
            throw new IllegalStateException("Order is not assigned to this group");
        }
    }

    @Override
    public Page<AgentDto> listAvailableAgents(Pageable pageable, Sort sort) {
        Page<User> users = userRepository.findByRole(UserRole.DELIVERY_AGENT, pageable);
        return users.map(this::mapToAgentDto);
    }

    @Override
    public Page<AgentDto> listAvailableAgents(String search, Pageable pageable, Sort sort) {
        log.info("Listing available agents with search='{}', page={}, size={}", 
                search, pageable.getPageNumber(), pageable.getPageSize());

        if (search == null || search.trim().isEmpty()) {
            return listAvailableAgents(pageable, sort);
        }

        Page<User> users = userRepository.findByRoleAndSearchTerm(
                UserRole.DELIVERY_AGENT, 
                search.trim().toLowerCase(), 
                pageable);
        return users.map(this::mapToAgentDto);
    }

    @Override
    public Optional<DeliveryGroupDto> findGroupByOrder(Long orderId) {
        log.info("Finding group for order: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        if (order.getReadyForDeliveryGroup() != null) {
            ReadyForDeliveryGroup group = groupRepository
                    .findByIdWithOrdersAndDeliverer(order.getReadyForDeliveryGroup().getDeliveryGroupId())
                    .orElse(null);
            if (group != null) {
                return Optional.of(mapToDeliveryGroupDto(group));
            }
        }

        return Optional.empty();
    }

    private DeliveryGroupDto mapToDeliveryGroupDto(ReadyForDeliveryGroup group) {
        List<Long> orderIds = group.getOrders().stream()
                .map(Order::getOrderId)
                .collect(Collectors.toList());

        String delivererName = group.getDeliverer() != null
                ? group.getDeliverer().getFirstName() + " " + group.getDeliverer().getLastName()
                : null;

        String status = "IN_PROGRESS";
        if(group.getHasDeliveryStarted() && group.getHasDeliveryFinished()){
            status = "COMPLETED";
        }
        else if (group.getHasDeliveryStarted() && !group.getHasDeliveryFinished()){
            status="DELIVERING";
        }
        else{
            status =  "READY";
        }
        return DeliveryGroupDto.builder()
                .deliveryGroupId(group.getDeliveryGroupId())
                .deliveryGroupName(group.getDeliveryGroupName())
                .deliveryGroupDescription(group.getDeliveryGroupDescription())
                .delivererId(group.getDeliverer() != null ? group.getDeliverer().getId() : null)
                .delivererName(delivererName)
                .orderIds(orderIds)
                .memberCount(group.getOrders().size())
                .createdAt(group.getCreatedAt())
                .scheduledAt(group.getScheduledAt())
                .hasDeliveryStarted(group.getHasDeliveryStarted())
                .deliveryStartedAt(group.getDeliveryStartedAt())
                .hasDeliveryFinished(group.getHasDeliveryFinished())
                .deliveryFinishedAt(group.getDeliveryFinishedAt())
                .status(status)
                .build();
    }

    private AgentDto mapToAgentDto(User user) {
        // Count only active (non-completed) delivery groups
        Long activeGroupCount = groupRepository.countActiveGroupsByDelivererId(user.getId());
        
        // Agent is busy if they have 5 or more active groups
        boolean hasAGroup = activeGroupCount >= 5;
        boolean isAvailable = activeGroupCount < 5;

        return AgentDto.builder()
                .agentId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getUserEmail())
                .phoneNumber(user.getPhoneNumber())
                .isAvailable(isAvailable)
                .hasAGroup(hasAGroup)
                .activeGroupCount(activeGroupCount)
                .lastActiveAt(user.getUpdatedAt())
                .build();
    }

    @Override
    public DeliveryAgentDashboardDTO getDeliveryAgentDashboard(UUID agentId) {
        log.info("Getting dashboard data for delivery agent: {}", agentId);

        // Get current groups (not finished)
        List<ReadyForDeliveryGroup> currentGroups = groupRepository
                .findByDelivererIdAndHasDeliveryFinishedFalse(agentId);

        // Get completed groups (finished)
        List<ReadyForDeliveryGroup> completedGroups = groupRepository
                .findByDelivererIdAndHasDeliveryFinishedTrue(agentId);

        // Calculate stats
        Long totalGroups = groupRepository.countByDelivererId(agentId);
        Long completedGroupsCount = (long) completedGroups.size();
        Long totalOrders = groupRepository.countOrdersByDelivererId(agentId);

        DeliveryAgentStatsDTO stats = new DeliveryAgentStatsDTO();
        stats.setTotalGroups(totalGroups);
        stats.setCompletedGroups(completedGroupsCount);
        stats.setTotalOrders(totalOrders);

        List<DeliveryGroupDto> currentGroupDtos = currentGroups.stream()
                .map(this::mapToDeliveryGroupDto)
                .collect(java.util.stream.Collectors.toList());

        List<DeliveryGroupDto> completedGroupDtos = completedGroups.stream()
                .map(this::mapToDeliveryGroupDto)
                .collect(java.util.stream.Collectors.toList());

        return DeliveryAgentDashboardDTO.builder()
                .stats(stats)
                .currentGroups(currentGroupDtos)
                .completedGroups(completedGroupDtos)
                .build();
    }

    @Override
    public List<OrderDTO> getOrdersForGroup(Long groupId, UUID agentId) {
        log.info("Getting orders for group {} by agent {}", groupId, agentId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrdersAndDeliverer(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found"));

        // Verify the agent owns this group
        if (!group.getDeliverer().getId().equals(agentId)) {
            throw new IllegalStateException("Agent does not have access to this group");
        }

        return group.getOrders().stream()
                .map(this::mapToOrderDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public DeliveryGroupDto startDelivery(Long groupId, UUID agentId) {
        log.info("Starting delivery for group {} by agent {}", groupId, agentId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithDeliverer(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found"));

        // Verify the agent owns this group
        if (!group.getDeliverer().getId().equals(agentId)) {
            throw new IllegalStateException("Agent does not have access to this group");
        }

        if (group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Delivery has already been started");
        }

        group.setHasDeliveryStarted(true);
        group.setDeliveryStartedAt(java.time.LocalDateTime.now());

        ReadyForDeliveryGroup savedGroup = groupRepository.save(group);

        // LOG ACTIVITY: Delivery Started for all orders in the group
        String deliveryAgentName = group.getDeliverer().getFirstName() + " " + group.getDeliverer().getLastName();
        ReadyForDeliveryGroup groupWithOrders = groupRepository.findByIdWithOrders(groupId)
            .orElseThrow(() -> new EntityNotFoundException("Delivery group not found"));
        
        for (Order order : groupWithOrders.getOrders()) {
            activityLogService.logDeliveryStarted(
                order.getOrderId(),
                group.getDeliveryGroupName(),
                deliveryAgentName,
                agentId.toString()
            );
        }

        return mapToDeliveryGroupDto(savedGroup);
    }

    @Override
    public DeliveryGroupDto finishDelivery(Long groupId, UUID agentId) {
        log.info("Finishing delivery for group {} by agent {}", groupId, agentId);

        ReadyForDeliveryGroup group = groupRepository.findByIdWithDeliverer(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found"));

        // Verify the agent owns this group
        if (!group.getDeliverer().getId().equals(agentId)) {
            throw new IllegalStateException("Agent does not have access to this group");
        }

        if (!group.getHasDeliveryStarted()) {
            throw new IllegalStateException("Delivery must be started before it can be finished");
        }

        if (group.getHasDeliveryFinished()) {
            throw new IllegalStateException("Delivery has already been finished");
        }

        group.setHasDeliveryFinished(true);
        group.setDeliveryFinishedAt(java.time.LocalDateTime.now());

        ReadyForDeliveryGroup savedGroup = groupRepository.save(group);
        return mapToDeliveryGroupDto(savedGroup);
    }

    @Override
    public OrderDTO getOrderDetailsForAgent(Long orderId, UUID agentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        if (order.getReadyForDeliveryGroup() == null ||
                !order.getReadyForDeliveryGroup().getDeliverer().getId().equals(agentId)) {
            throw new IllegalStateException("Agent does not have access to this order");
        }

        return mapToOrderDTO(order);
    }

    private OrderDTO mapToOrderDTO(Order order) {
        List<OrderItemDTO> orderItems = order.getOrderItems().stream()
                .map(item -> {
                    OrderItemDTO dto = new OrderItemDTO();
                    dto.setId(item.getOrderItemId().toString());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    dto.setTotalPrice(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));

                    SimpleProductDTO productDto = new SimpleProductDTO();

                    if (item.getProductVariant() != null) {
                        dto.setVariantId(item.getProductVariant().getId().toString());
                        dto.setProductId(item.getProductVariant().getProduct().getProductId().toString());
                        productDto.setProductId(item.getProductVariant().getProduct().getProductId().toString());
                        productDto.setName(item.getProductVariant().getProduct().getProductName() +
                                " - " + item.getProductVariant().getVariantName());
                        productDto.setImages(item.getProductVariant().getProduct().getImages().stream()
                                .map(img -> img.getImageUrl())
                                .toArray(String[]::new));
                    } else if (item.getProduct() != null) {
                        // Handle regular product items
                        dto.setProductId(item.getProduct().getProductId().toString());
                        productDto.setProductId(item.getProduct().getProductId().toString());
                        productDto.setName(item.getProduct().getProductName());
                        productDto.setImages(item.getProduct().getImages().stream()
                                .map(img -> img.getImageUrl())
                                .toArray(String[]::new));
                    }

                    dto.setProduct(productDto);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

        AddressDto shippingAddress = new AddressDto();
        if (order.getOrderAddress() != null) {
            shippingAddress.setStreetAddress(order.getOrderAddress().getStreet() != null ? order.getOrderAddress().getStreet() : "N/A");
            shippingAddress.setCity(order.getOrderAddress().getRegions() != null ? order.getOrderAddress().getRegions() : "N/A");
            shippingAddress.setState(order.getOrderAddress().getRegions() != null ? order.getOrderAddress().getRegions() : "N/A");
            shippingAddress.setCountry(order.getOrderAddress().getCountry() != null ? order.getOrderAddress().getCountry() : "N/A");
            shippingAddress.setLatitude(order.getOrderAddress().getLatitude());
            shippingAddress.setLongitude(order.getOrderAddress().getLongitude());
        } else {
            shippingAddress.setStreetAddress("N/A");
            shippingAddress.setCity("N/A");
            shippingAddress.setState("N/A");
            shippingAddress.setCountry("N/A");
            shippingAddress.setLatitude(null);
            shippingAddress.setLongitude(null);
        }

        String customerName = "N/A";
        String customerEmail = "N/A";
        String customerPhone = "N/A";

        if (order.getOrderCustomerInfo() != null) {
            String firstName = order.getOrderCustomerInfo().getFirstName() != null
                    ? order.getOrderCustomerInfo().getFirstName()
                    : "";
            String lastName = order.getOrderCustomerInfo().getLastName() != null
                    ? order.getOrderCustomerInfo().getLastName()
                    : "";
            customerName = (firstName + " " + lastName).trim();
            if (customerName.isEmpty())
                customerName = "N/A";

            customerEmail = order.getOrderCustomerInfo().getEmail() != null ? order.getOrderCustomerInfo().getEmail()
                    : "N/A";
            customerPhone = order.getOrderCustomerInfo().getPhoneNumber() != null
                    ? order.getOrderCustomerInfo().getPhoneNumber()
                    : "N/A";
        }

        // Handle order info safely
        java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        if (order.getOrderInfo() != null && order.getOrderInfo().getTotalAmount() != null) {
            totalAmount = order.getOrderInfo().getTotalAmount();
        }

        // Calculate total items count (sum of all quantities)
        int totalItemsCount = orderItems.stream()
                .mapToInt(OrderItemDTO::getQuantity)
                .sum();

        return OrderDTO.builder()
                .id(order.getOrderId())
                .orderNumber(order.getOrderCode())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .status(order.getOrderStatus() != null ? order.getOrderStatus().toString() : "UNKNOWN")
                .totalAmount(totalAmount)
                .totalItems(totalItemsCount)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(orderItems)
                .shippingAddress(shippingAddress)
                .build();
    }

    @Override
    public Map<String, Object> startDelivery(Long groupId) {
        log.info("Starting delivery for group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found with id: " + groupId));

        // Check if delivery has already started
        if (Boolean.TRUE.equals(group.getHasDeliveryStarted())) {
            throw new IllegalStateException("Delivery has already been started for this group");
        }

        // Check if all orders are not delivered
        List<Order> orders = group.getOrders();
        long deliveredOrders = orders.stream()
                .filter(order -> order.getOrderStatus() == Order.OrderStatus.DELIVERED)
                .count();

        if (deliveredOrders > 0) {
            throw new IllegalStateException(
                    "Cannot start delivery: " + deliveredOrders + " orders are already delivered");
        }

        // Start delivery
        group.setHasDeliveryStarted(true);
        group.setDeliveryStartedAt(java.time.LocalDateTime.now());
        groupRepository.save(group);

        // Log activity for all orders in the group
        String agentName = group.getDeliverer() != null ? 
            group.getDeliverer().getFirstName() + " " + group.getDeliverer().getLastName() : 
            "Delivery Agent";
        
        for (Order order : orders) {
            activityLogService.logDeliveryStarted(
                order.getOrderId(),
                group.getDeliveryGroupName(),
                agentName,
                group.getDeliverer() != null ? group.getDeliverer().getId().toString() : null
            );
        }

        // Send email notifications to customers (async)
        sendDeliveryStartNotifications(group);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("groupName", group.getDeliveryGroupName());
        result.put("deliveryStartedAt", group.getDeliveryStartedAt());
        result.put("totalOrders", orders.size());
        result.put("notificationsSent", orders.size());

        log.info("Delivery started successfully for group: {} with {} orders", groupId, orders.size());
        return result;
    }

    @Override
    public Map<String, Object> finishDelivery(Long groupId) {
        log.info("Finishing delivery for group: {}", groupId);

        ReadyForDeliveryGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found with id: " + groupId));

        // Check if delivery has been started
        if (!Boolean.TRUE.equals(group.getHasDeliveryStarted())) {
            throw new IllegalStateException("Delivery has not been started for this group");
        }

        // Check if delivery has already finished
        if (Boolean.TRUE.equals(group.getHasDeliveryFinished())) {
            throw new IllegalStateException("Delivery has already been finished for this group");
        }

        // Check if all orders are delivered and pickup tokens are used
        List<Order> orders = group.getOrders();
        long undeliveredOrders = orders.stream()
                .filter(order -> order.getOrderStatus() != Order.OrderStatus.DELIVERED)
                .count();

        long unusedTokens = orders.stream()
                .filter(order -> !Boolean.TRUE.equals(order.getPickupTokenUsed()))
                .count();

        if (undeliveredOrders > 0) {
            throw new IllegalStateException(
                    "Cannot finish delivery: " + undeliveredOrders + " orders are not yet delivered");
        }

        if (unusedTokens > 0) {
            throw new IllegalStateException(
                    "Cannot finish delivery: " + unusedTokens + " pickup tokens are not yet used");
        }

        // Finish delivery
        group.setHasDeliveryFinished(true);
        group.setDeliveryFinishedAt(java.time.LocalDateTime.now());
        groupRepository.save(group);

        Map<String, Object> result = new HashMap<>();
        result.put("groupId", groupId);
        result.put("groupName", group.getDeliveryGroupName());
        result.put("deliveryFinishedAt", group.getDeliveryFinishedAt());
        result.put("totalOrders", orders.size());
        result.put("deliveredOrders", orders.size());

        log.info("Delivery finished successfully for group: {} with {} orders", groupId, orders.size());
        return result;
    }

    private void sendDeliveryStartNotifications(ReadyForDeliveryGroup group) {
        // Use async processing to send emails quickly
        CompletableFuture.runAsync(() -> {
            try {
                List<Order> orders = group.getOrders();
                log.info("Sending delivery start notifications for {} orders", orders.size());

                for (Order order : orders) {
                    try {
                        if (order.getOrderStatus() != Order.OrderStatus.DELIVERED && order.getUser() != null) {
                            sendDeliveryStartEmail(order);
                        }
                    } catch (Exception e) {
                        log.error("Failed to send notification for order {}: {}", order.getOrderId(), e.getMessage());
                    }
                }

                log.info("Completed sending delivery start notifications for group: {}", group.getDeliveryGroupId());
            } catch (Exception e) {
                log.error("Error sending delivery start notifications: {}", e.getMessage(), e);
            }
        });
    }

    private void sendDeliveryStartEmail(Order order) {
        try {
            // Create email content
            String subject = "Your Order Delivery Has Started - " + order.getOrderCode();
            String content = createDeliveryStartEmailContent(order);

            // Send email (you can integrate with your email service here)
            log.info("Sending delivery start email to: {} for order: {}",
                    order.getUser().getUserEmail(), order.getOrderCode());

            // TODO: Integrate with actual email service (e.g., SendGrid, AWS SES, etc.)
            // emailService.sendEmail(order.getUser().getUserEmail(), subject, content);

        } catch (Exception e) {
            log.error("Failed to send delivery start email for order {}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private String createDeliveryStartEmailContent(Order order) {
        StringBuilder content = new StringBuilder();
        content.append("<html><body>");
        content.append("<h2>Your Order Delivery Has Started!</h2>");
        content.append("<p>Dear ").append(order.getUser().getFirstName()).append(",</p>");
        content.append("<p>Great news! Your order <strong>").append(order.getOrderCode())
                .append("</strong> is now out for delivery.</p>");

        content.append("<h3>Order Summary:</h3>");
        content.append("<ul>");
        for (OrderItem item : order.getOrderItems()) {
            content.append("<li>").append(item.getQuantity()).append("x ");
            if (item.getProductVariant() != null) {
                content.append(item.getProductVariant().getProduct().getProductName());
            } else {
                content.append("Product");
            }
            content.append("</li>");
        }
        content.append("</ul>");

        content.append("<p><strong>Total Amount:</strong> $").append(order.getTotalAmount()).append("</p>");

        content.append("<h3>Delivery Information:</h3>");
        if (order.getOrderAddress() != null) {
            content.append("<p><strong>Delivery Address:</strong><br>");
            content.append(order.getOrderAddress().getStreet()).append("<br>");
            content.append(order.getOrderAddress().getRegions()).append(" ");
            content.append(order.getOrderAddress().getCountry()).append("</p>");
        }

        content.append("<p>Please have your pickup QR code ready when the delivery agent arrives.</p>");
        content.append("<p>Thank you for choosing our service!</p>");
        content.append("</body></html>");

        return content.toString();
    }

    @Override
    @Transactional
    public DeliveryGroupDto changeOrderGroup(Long orderId, Long newGroupId) {
        log.info("Changing order {} to group {}", orderId, newGroupId);

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

        // Find the new group
        ReadyForDeliveryGroup newGroup = groupRepository.findByIdWithDeliverer(newGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery group not found with ID: " + newGroupId));

        // Validate that the new group hasn't started delivery
        if (newGroup.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot assign order to a group that has already started delivery");
        }

        // Get the old group if exists
        ReadyForDeliveryGroup oldGroup = order.getReadyForDeliveryGroup();

        // Validate that the old group (if exists) hasn't started delivery
        if (oldGroup != null && oldGroup.getHasDeliveryStarted()) {
            throw new IllegalStateException("Cannot change order from a group that has already started delivery");
        }

        // Remove from old group if exists
        if (oldGroup != null) {
            oldGroup.getOrders().remove(order);
            groupRepository.save(oldGroup);
            log.info("Removed order {} from old group {}", orderId, oldGroup.getDeliveryGroupId());
        }

        // Add to new group
        order.setReadyForDeliveryGroup(newGroup);
        newGroup.getOrders().add(order);
        
        // Save both entities
        orderRepository.save(order);
        ReadyForDeliveryGroup savedGroup = groupRepository.save(newGroup);

        log.info("Successfully changed order {} to group {}", orderId, newGroupId);

        return mapToDeliveryGroupDto(savedGroup);
    }
}
