package com.ecommerce.service;

import com.ecommerce.dto.CreateDeliveryNoteRequest;
import com.ecommerce.dto.OrderDeliveryNoteDTO;
import com.ecommerce.dto.UpdateDeliveryNoteRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderDeliveryNoteService {

    /**
     * Create a new delivery note
     * @param request The note creation request
     * @param agentId The ID of the delivery agent creating the note
     * @return The created note DTO
     */
    OrderDeliveryNoteDTO createNote(CreateDeliveryNoteRequest request, String agentId);

    /**
     * Update an existing delivery note
     * @param noteId The ID of the note to update
     * @param request The update request
     * @param agentId The ID of the agent attempting the update
     * @return The updated note DTO
     */
    OrderDeliveryNoteDTO updateNote(Long noteId, UpdateDeliveryNoteRequest request, String agentId);

    /**
     * Delete a delivery note (soft delete)
     * @param noteId The ID of the note to delete
     * @param agentId The ID of the agent attempting the deletion
     */
    void deleteNote(Long noteId, String agentId);

    /**
     * Get all notes for a specific order
     * @param orderId The order ID
     * @param pageable Pagination information
     * @return Page of notes
     */
    Page<OrderDeliveryNoteDTO> getNotesForOrder(Long orderId, Pageable pageable);

    /**
     * Get all notes for a specific delivery group
     * @param groupId The delivery group ID
     * @param pageable Pagination information
     * @return Page of notes
     */
    Page<OrderDeliveryNoteDTO> getNotesForDeliveryGroup(Long groupId, Pageable pageable);

    /**
     * Get all notes by a specific agent
     * @param agentId The agent ID
     * @param pageable Pagination information
     * @return Page of notes
     */
    Page<OrderDeliveryNoteDTO> getNotesByAgent(String agentId, Pageable pageable);

    /**
     * Get all notes (order-specific and group-general) for a delivery group
     * @param groupId The delivery group ID
     * @param pageable Pagination information
     * @return Page of notes
     */
    Page<OrderDeliveryNoteDTO> getAllNotesForDeliveryGroup(Long groupId, Pageable pageable);

    /**
     * Get a specific note by ID
     * @param noteId The note ID
     * @return The note DTO
     */
    OrderDeliveryNoteDTO getNoteById(Long noteId);
}
