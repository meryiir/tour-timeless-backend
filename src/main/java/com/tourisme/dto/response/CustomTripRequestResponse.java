package com.tourisme.dto.response;

import com.tourisme.entity.CustomTripRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomTripRequestResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String startCity;
    private String destinationCity;
    private LocalDate preferredDate;
    private Integer numberOfPeople;
    private String message;
    private CustomTripRequest.Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

