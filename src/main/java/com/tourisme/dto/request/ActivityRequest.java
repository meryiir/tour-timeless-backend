package com.tourisme.dto.request;

import com.tourisme.entity.Activity.DifficultyLevel;
import com.tourisme.entity.Activity.TourType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;
    
    private String fullDescription;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Premium price must be greater than 0")
    private BigDecimal premiumPrice;
    
    @DecimalMin(value = "0.0", inclusive = false, message = "Budget price must be greater than 0")
    private BigDecimal budgetPrice;
    
    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;
    
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;
    
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
    
    private DifficultyLevel difficultyLevel;
    
    private TourType tourType;
    
    @NotNull(message = "Destination ID is required")
    private Long destinationId;
    
    private Boolean featured;
    
    private Boolean active;
    
    @Min(value = 1, message = "Max group size must be at least 1")
    private Integer maxGroupSize;
    
    @Min(value = 0, message = "Available slots must be at least 0")
    private Integer availableSlots;
    
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;
    
    private List<String> galleryImages = new ArrayList<>();
    
    private List<String> includedItems = new ArrayList<>();
    
    private List<String> excludedItems = new ArrayList<>();
    
    private List<String> itinerary = new ArrayList<>();
    
    private List<LocalDate> availableDates = new ArrayList<>();
    
    @Size(max = 255, message = "Departure location must not exceed 255 characters")
    private String departureLocation;
    
    @Size(max = 255, message = "Return location must not exceed 255 characters")
    private String returnLocation;
    
    @Size(max = 255, message = "Meeting time must not exceed 255 characters")
    private String meetingTime;
    
    @Size(max = 100, message = "Availability must not exceed 100 characters")
    private String availability;
    
    private String whatToExpect;
    
    private List<String> complementaries = new ArrayList<>();
    
    @Size(max = 500, message = "Map URL must not exceed 500 characters")
    private String mapUrl;
    
    private List<ActivityTranslationRequest> translations = new ArrayList<>();
}
