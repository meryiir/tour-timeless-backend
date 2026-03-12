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
public class DestinationResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String fullDescription;
    private String imageUrl;
    private String country;
    private String city;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
