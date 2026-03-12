package com.tourisme.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingRequest {
    
    @NotNull(message = "Activity ID is required")
    private Long activityId;
    
    @NotNull(message = "Travel date is required")
    @Future(message = "Travel date must be in the future")
    private LocalDate travelDate;
    
    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "Number of people must be at least 1")
    private Integer numberOfPeople;
    
    private String specialRequest;
}
