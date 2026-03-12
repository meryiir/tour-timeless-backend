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
public class ReviewResponse {
    private Long id;
    private UserResponse user;
    private ActivityResponse activity;
    private Integer rating;
    private String comment;
    private Boolean approved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
