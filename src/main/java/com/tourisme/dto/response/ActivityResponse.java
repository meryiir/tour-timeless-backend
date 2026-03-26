package com.tourisme.dto.response;

import com.tourisme.entity.Activity.DifficultyLevel;
import com.tourisme.entity.Activity.TourType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityResponse {
    private Long id;
    private String title;
    private String slug;
    private String shortDescription;
    private String fullDescription;
    private BigDecimal price;
    private BigDecimal premiumPrice;
    private BigDecimal budgetPrice;
    private String duration;
    private String location;
    private String category;
    private DifficultyLevel difficultyLevel;
    private TourType tourType;
    private BigDecimal ratingAverage;
    private Integer reviewCount;
    private Boolean featured;
    private Boolean active;
    private Integer maxGroupSize;
    private Integer availableSlots;
    private String imageUrl;
    private List<String> galleryImages = new ArrayList<>();
    private List<String> includedItems = new ArrayList<>();
    private List<String> excludedItems = new ArrayList<>();
    private List<String> itinerary = new ArrayList<>();
    private List<LocalDate> availableDates = new ArrayList<>();
    private String departureLocation;
    private String returnLocation;
    private String meetingTime;
    private String availability;
    private String whatToExpect;
    private List<String> complementaries = new ArrayList<>();
    private String mapUrl;
    /** FK for admin forms when {@link #destination} is null or not in a dropdown page of results. */
    private Long destinationId;
    private DestinationResponse destination;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
