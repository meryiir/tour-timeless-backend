package com.tourisme.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ActivityTranslationRequest {
    
    @NotBlank(message = "Language code is required")
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;
    
    private String fullDescription;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    @Size(max = 255, message = "Departure location must not exceed 255 characters")
    private String departureLocation;
    
    @Size(max = 255, message = "Return location must not exceed 255 characters")
    private String returnLocation;
    
    @Size(max = 255, message = "Meeting time must not exceed 255 characters")
    private String meetingTime;
    
    @Size(max = 100, message = "Availability must not exceed 100 characters")
    private String availability;
    
    private String whatToExpect;
}
