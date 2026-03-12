package com.tourisme.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {
    
    @NotNull(message = "Activity ID is required")
    private Long activityId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    
    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
