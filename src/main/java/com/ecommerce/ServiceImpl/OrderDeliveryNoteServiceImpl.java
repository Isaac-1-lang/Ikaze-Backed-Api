package com.ecommerce.ServiceImpl;

import com.ecommerce.dto.CreateDeliveryNoteRequest;
import com.ecommerce.dto.OrderDeliveryNoteDTO;
import com.ecommerce.dto.UpdateDeliveryNoteRequest;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderDeliveryNote;
import com.ecommerce.entity.ReadyForDeliveryGroup;
import com.ecommerce.entity.User;
import com.ecommerce.Exception.ResourceNotFoundException;
import com.ecommerce.Exception.UnauthorizedException;
import com.ecommerce.repository.OrderDeliveryNoteRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ReadyForDeliveryGroupRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.OrderDeliveryNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDeliveryNoteServiceImpl implements OrderDeliveryNoteService {

    private final OrderDeliveryNoteRepository noteRepository;
    private final OrderRepository orderRepository;
    private final ReadyForDeliveryGroupRepository deliveryGroupRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OrderDeliveryNoteDTO createNote(CreateDeliveryNoteRequest request, String agentEmail) {
        User agent = userRepository.findByUserEmail(agentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with ID: " + agentEmail));
        OrderDeliveryNote.NoteType noteType;
        try {
            noteType = OrderDeliveryNote.NoteType.valueOf(request.getNoteType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid note type: " + request.getNoteType());
        }

        OrderDeliveryNote.NoteCategory noteCategory = null;
        if (request.getNoteCategory() != null && !request.getNoteCategory().isEmpty()) {
            try {
                noteCategory = OrderDeliveryNote.NoteCategory.valueOf(request.getNoteCategory());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid note category: " + request.getNoteCategory());
            }
        }

        OrderDeliveryNote note = new OrderDeliveryNote();
        note.setNoteText(request.getNoteText());
        note.setNoteType(noteType);
        note.setNoteCategory(noteCategory);
        note.setAgent(agent);

        if (noteType == OrderDeliveryNote.NoteType.ORDER_SPECIFIC) {
            if (request.getOrderId() == null) {
                throw new IllegalArgumentException("Order ID is required for order-specific notes");
            }

            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + request.getOrderId()));

            // Verify order status is PROCESSING
            if (order.getOrderStatus() != Order.OrderStatus.PROCESSING) {
                throw new IllegalStateException("Notes can only be created for orders with PROCESSING status. Current status: " + order.getOrderStatus());
            }

            if (order.getReadyForDeliveryGroup() == null) {
                throw new IllegalStateException("Order is not assigned to any delivery group");
            }

            if (order.getReadyForDeliveryGroup().getDeliverer() == null ||
                !order.getReadyForDeliveryGroup().getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are not assigned to deliver this order");
            }

            note.setOrder(order);
        }
        else if (noteType == OrderDeliveryNote.NoteType.GROUP_GENERAL) {
            if (request.getDeliveryGroupId() == null) {
                throw new IllegalArgumentException("Delivery group ID is required for group-general notes");
            }

            ReadyForDeliveryGroup group = deliveryGroupRepository.findById(request.getDeliveryGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Delivery group not found with ID: " + request.getDeliveryGroupId()));

            // Verify delivery has started but not finished
            if (!group.getHasDeliveryStarted()) {
                throw new IllegalStateException("Notes can only be created for deliveries that have started. Delivery has not started yet.");
            }
            if (group.getHasDeliveryFinished()) {
                throw new IllegalStateException("Notes cannot be created for completed deliveries. This delivery has already finished.");
            }

            if (group.getDeliverer() == null || !group.getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are not assigned to this delivery group");
            }

            note.setDeliveryGroup(group);
        }

        OrderDeliveryNote savedNote = noteRepository.save(note);
        log.info("Delivery note created successfully with ID: {}", savedNote.getNoteId());

        return OrderDeliveryNoteDTO.fromEntity(savedNote);
    }

    @Override
    @Transactional
    public OrderDeliveryNoteDTO updateNote(Long noteId, UpdateDeliveryNoteRequest request, String agentEmail) {
        User agent = userRepository.findByUserEmail(agentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with email: " + agentEmail));
        
        OrderDeliveryNote note = noteRepository.findByIdNotDeleted(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery note not found with ID: " + noteId));

        if (!note.getAgent().getId().equals(agent.getId())) {
            throw new UnauthorizedException("You can only update notes that you created");
        }

        if (note.getOrder() != null) {
            ReadyForDeliveryGroup group = note.getOrder().getReadyForDeliveryGroup();
            if (group == null || group.getDeliverer() == null ||
                !group.getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are no longer assigned to this order's delivery");
            }
        } else if (note.getDeliveryGroup() != null) {
            if (note.getDeliveryGroup().getDeliverer() == null ||
                !note.getDeliveryGroup().getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are no longer assigned to this delivery group");
            }
        }

        // Update note
        note.setNoteText(request.getNoteText());

        // Update category if provided
        if (request.getNoteCategory() != null && !request.getNoteCategory().isEmpty()) {
            try {
                OrderDeliveryNote.NoteCategory category = OrderDeliveryNote.NoteCategory.valueOf(request.getNoteCategory());
                note.setNoteCategory(category);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid note category: " + request.getNoteCategory());
            }
        }

        OrderDeliveryNote updatedNote = noteRepository.save(note);
        log.info("Delivery note {} updated successfully by agent: {}", noteId, agentEmail);

        return OrderDeliveryNoteDTO.fromEntity(updatedNote);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, String agentEmail) {
        User agent = userRepository.findByUserEmail(agentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with email: " + agentEmail));
        
        OrderDeliveryNote note = noteRepository.findByIdNotDeleted(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery note not found with ID: " + noteId));

        if (!note.getAgent().getId().equals(agent.getId())) {
            throw new UnauthorizedException("You can only delete notes that you created");
        }

        if (note.getOrder() != null) {
            ReadyForDeliveryGroup group = note.getOrder().getReadyForDeliveryGroup();
            if (group == null || group.getDeliverer() == null ||
                !group.getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are no longer assigned to this order's delivery");
            }
        } else if (note.getDeliveryGroup() != null) {
            if (note.getDeliveryGroup().getDeliverer() == null ||
                !note.getDeliveryGroup().getDeliverer().getId().equals(agent.getId())) {
                throw new UnauthorizedException("You are no longer assigned to this delivery group");
            }
        }

        // Soft delete
        note.setIsDeleted(true);
        noteRepository.save(note);

        log.info("Delivery note {} deleted successfully by agent: {}", noteId, agentEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDeliveryNoteDTO> getNotesForOrder(Long orderId, Pageable pageable) {
        log.info("Fetching notes for order: {}", orderId);

        // Verify order exists
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }

        Page<OrderDeliveryNote> notes = noteRepository.findByOrderId(orderId, pageable);
        return notes.map(OrderDeliveryNoteDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDeliveryNoteDTO> getNotesForDeliveryGroup(Long groupId, Pageable pageable) {
        log.info("Fetching notes for delivery group: {}", groupId);

        // Verify group exists
        if (!deliveryGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Delivery group not found with ID: " + groupId);
        }

        Page<OrderDeliveryNote> notes = noteRepository.findByDeliveryGroupId(groupId, pageable);
        return notes.map(OrderDeliveryNoteDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDeliveryNoteDTO> getNotesByAgent(String agentEmail, Pageable pageable) {
        User agent = userRepository.findByUserEmail(agentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found with email: " + agentEmail));

        Page<OrderDeliveryNote> notes = noteRepository.findByAgentId(agent.getId().toString(), pageable);
        return notes.map(OrderDeliveryNoteDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDeliveryNoteDTO> getAllNotesForDeliveryGroup(Long groupId, Pageable pageable) {
        log.info("Fetching all notes (order-specific and group-general) for delivery group: {}", groupId);

        // Verify group exists
        if (!deliveryGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Delivery group not found with ID: " + groupId);
        }

        Page<OrderDeliveryNote> notes = noteRepository.findAllNotesForDeliveryGroup(groupId, pageable);
        return notes.map(OrderDeliveryNoteDTO::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDeliveryNoteDTO getNoteById(Long noteId) {
        log.info("Fetching note by ID: {}", noteId);

        OrderDeliveryNote note = noteRepository.findByIdNotDeleted(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery note not found with ID: " + noteId));

        return OrderDeliveryNoteDTO.fromEntity(note);
    }
}
