package com.tourisme.mapper;

import com.tourisme.dto.response.ActivityResponse;
import com.tourisme.entity.Activity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityMapper {
    
    private final DestinationMapper destinationMapper;
    
    public ActivityResponse toResponse(Activity activity) {
        if (activity == null) {
            return null;
        }
        
        return ActivityResponse.builder()
                .id(activity.getId())
                .title(activity.getTitle())
                .slug(activity.getSlug())
                .shortDescription(activity.getShortDescription())
                .fullDescription(activity.getFullDescription())
                .price(activity.getPrice())
                .duration(activity.getDuration())
                .location(activity.getLocation())
                .category(activity.getCategory())
                .difficultyLevel(activity.getDifficultyLevel())
                .ratingAverage(activity.getRatingAverage())
                .reviewCount(activity.getReviewCount())
                .featured(activity.getFeatured())
                .active(activity.getActive())
                .maxGroupSize(activity.getMaxGroupSize())
                .availableSlots(activity.getAvailableSlots())
                .imageUrl(activity.getImageUrl())
                .galleryImages(activity.getGalleryImages() != null ? activity.getGalleryImages() : new java.util.ArrayList<>())
                .includedItems(activity.getIncludedItems() != null ? activity.getIncludedItems() : new java.util.ArrayList<>())
                .excludedItems(activity.getExcludedItems() != null ? activity.getExcludedItems() : new java.util.ArrayList<>())
                .itinerary(activity.getItinerary() != null ? activity.getItinerary() : new java.util.ArrayList<>())
                .availableDates(activity.getAvailableDates() != null ? activity.getAvailableDates() : new java.util.ArrayList<>())
                .departureLocation(activity.getDepartureLocation())
                .returnLocation(activity.getReturnLocation())
                .meetingTime(activity.getMeetingTime())
                .availability(activity.getAvailability())
                .whatToExpect(activity.getWhatToExpect())
                .complementaries(activity.getComplementaries() != null ? activity.getComplementaries() : new java.util.ArrayList<>())
                .mapUrl(activity.getMapUrl())
                .destination(destinationMapper.toResponse(activity.getDestination()))
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
