package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDto {
    private java.util.UUID agentId;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private boolean isAvailable;
    private boolean hasAGroup;
    private Long activeGroupCount;
    private LocalDateTime lastActiveAt;
}
