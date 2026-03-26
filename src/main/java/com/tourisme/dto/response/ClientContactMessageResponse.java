package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientContactMessageResponse {
    private Long id;
    private String subject;
    private String message;
    private String adminReply;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
}
