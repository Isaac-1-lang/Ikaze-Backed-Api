package com.ecommerce.service.impl;

import com.ecommerce.Enum.UserRole;
import com.ecommerce.dto.*;
import com.ecommerce.entity.Order;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadyForDeliveryGroupServiceImpl implements ReadyForDeliveryGroupService {

    private final ReadyForDeliveryGroupRepository groupRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ReadyForDeliveryGroupDTO createGroup(CreateReadyForDeliveryGroupDTO request) {
        log.info("Creating ready for delivery group: {}", request.getDeliveryGroupName());

        User deliverer = userRepository.findById(request.getDelivererId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Deliverer not found with ID: " + request.getDelivererId()));
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

    private void addOrdersToGroupInternal(Long groupId, List<Long> orderIds) {
        ReadyForDeliveryGroup group = groupRepository.findByIdWithOrders(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with ID: " + groupId));

        for (Long orderId : orderIds) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found with ID: " + orderId));

            if (order.getReadyForDeliveryGroup() != null) {
                throw new IllegalStateException("Order " + orderId + " is already assigned to another delivery group");
            }

            group.addOrder(order);
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

        return ReadyForDeliveryGroupDTO.builder()
                .deliveryGroupId(group.getDeliveryGroupId())
                .deliveryGroupName(group.getDeliveryGroupName())
                .deliveryGroupDescription(group.getDeliveryGroupDescription())
                .delivererId(group.getDeliverer() != null ? group.getDeliverer().getId() : null)
                .delivererName(delivererName)
                .orderIds(orderIds)
                .orderCount(group.getOrders().size())
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
        log.info("Listing available agents with pagination: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());

        Page<User> users = userRepository.findByRole(UserRole.DELIVERY_AGENT, pageable);
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

        String status = group.getHasDeliveryStarted() ? "IN_PROGRESS" : "READY";

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
        Long currentGroupCount = groupRepository.countByDelivererId(user.getId());
        boolean hasAGroup = currentGroupCount > 0;

        return AgentDto.builder()
                .agentId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getUserEmail())
                .phoneNumber(user.getPhoneNumber())
                .isAvailable(!hasAGroup) // Agent is available if they don't have a group
                .hasAGroup(hasAGroup)
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
        log.info("Getting order details for order {} by agent {}", orderId, agentId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found"));

        // Verify the agent has access to this order through a delivery group
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
                    dto.setProductId(item.getProduct().getProductId().toString());
                    dto.setQuantity(item.getQuantity());
                    dto.setPrice(item.getPrice());
                    dto.setTotalPrice(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));

                    SimpleProductDTO productDto = new SimpleProductDTO();
                    productDto.setProductId(item.getProduct().getProductId().toString());
                    productDto.setName(item.getProduct().getProductName());
                    productDto.setImages(item.getProduct().getImages().stream()
                            .map(img -> img.getImageUrl())
                            .toArray(String[]::new));
                    dto.setProduct(productDto);

                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());

        AddressDto shippingAddress = new AddressDto();
        if (order.getOrderAddress() != null) {
            shippingAddress.setStreetAddress(order.getOrderAddress().getStreet());
            shippingAddress.setCity(order.getOrderAddress().getRegions());
            shippingAddress.setState(order.getOrderAddress().getRegions());
            shippingAddress.setPostalCode(order.getOrderAddress().getZipcode());
            shippingAddress.setCountry(order.getOrderAddress().getCountry());
        } else {
            shippingAddress.setStreetAddress("N/A");
            shippingAddress.setCity("N/A");
            shippingAddress.setState("N/A");
            shippingAddress.setPostalCode("N/A");
            shippingAddress.setCountry("N/A");
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

        return OrderDTO.builder()
                .id(order.getOrderId())
                .orderNumber(order.getOrderCode())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .customerPhone(customerPhone)
                .status(order.getOrderStatus() != null ? order.getOrderStatus().toString() : "UNKNOWN")
                .totalAmount(totalAmount)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(orderItems)
                .shippingAddress(shippingAddress)
                .build();
    }
}
