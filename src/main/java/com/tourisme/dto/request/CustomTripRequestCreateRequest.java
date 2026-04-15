package com.tourisme.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomTripRequestCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    private String email;

    private String phone;

    @NotBlank(message = "Start city is required")
    private String startCity;

    @NotBlank(message = "Destination city is required")
    private String destinationCity;

    /** Optional preferred travel date (can be null). */
    private LocalDate preferredDate;

    @Min(value = 1, message = "Number of people must be at least 1")
    private Integer numberOfPeople;

    private String message;
}

