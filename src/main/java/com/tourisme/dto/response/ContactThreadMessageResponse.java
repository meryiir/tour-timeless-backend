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
public class ContactThreadMessageResponse {
    private Long id;
    /** CLIENT or ADMIN */
    private String sender;
    private String body;
    private LocalDateTime createdAt;
}

