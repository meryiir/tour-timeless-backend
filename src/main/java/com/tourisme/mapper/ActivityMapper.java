package com.tourisme.mapper;

import com.tourisme.dto.response.ActivityResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.ActivityTranslation;
import com.tourisme.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ActivityMapper {
    
    private final DestinationMapper destinationMapper;
    private final TranslationService translationService;
    
    public ActivityResponse toResponse(Activity activity) {
        return toResponse(activity, "en");
    }
    
    public ActivityResponse toResponse(Activity activity, String languageCode) {
        if (activity == null) {
            return null;
        }
        
        ActivityTranslation translation = translationService.getActivityTranslation(activity, languageCode);
        
        return ActivityResponse.builder()
                .id(activity.getId())
                .title(translation.getTitle())
                .slug(activity.getSlug())
                .shortDescription(translation.getShortDescription())
                .fullDescription(translation.getFullDescription())
                .price(activity.getPrice())
                .premiumPrice(activity.getPremiumPrice())
                .budgetPrice(activity.getBudgetPrice())
                .duration(activity.getDuration())
                .location(translation.getLocation())
                .category(translation.getCategory())
                .difficultyLevel(activity.getDifficultyLevel())
                .tourType(activity.getTourType())
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
                .departureLocation(translation.getDepartureLocation())
                .returnLocation(translation.getReturnLocation())
                .meetingTime(translation.getMeetingTime())
                .availability(translation.getAvailability())
                .whatToExpect(translation.getWhatToExpect())
                .complementaries(activity.getComplementaries() != null ? activity.getComplementaries() : new java.util.ArrayList<>())
                .mapUrl(activity.getMapUrl())
                .destinationId(activity.getDestination() != null ? activity.getDestination().getId() : null)
                .destination(destinationMapper.toResponse(activity.getDestination(), languageCode))
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }

    /**
     * Scalars only — skips lazy {@link jakarta.persistence.ElementCollection} fields.
     * Use for nested activity inside bookings/reviews when open-in-view is disabled.
     */
    public ActivityResponse toSummaryResponse(Activity activity) {
        return toSummaryResponse(activity, "en");
    }

    public ActivityResponse toSummaryResponse(Activity activity, String languageCode) {
        if (activity == null) {
            return null;
        }

        ActivityTranslation translation = translationService.getActivityTranslation(activity, languageCode);

        return ActivityResponse.builder()
                .id(activity.getId())
                .title(translation.getTitle())
                .slug(activity.getSlug())
                .shortDescription(translation.getShortDescription())
                .fullDescription(translation.getFullDescription())
                .price(activity.getPrice())
                .premiumPrice(activity.getPremiumPrice())
                .budgetPrice(activity.getBudgetPrice())
                .duration(activity.getDuration())
                .location(translation.getLocation())
                .category(translation.getCategory())
                .difficultyLevel(activity.getDifficultyLevel())
                .tourType(activity.getTourType())
                .ratingAverage(activity.getRatingAverage())
                .reviewCount(activity.getReviewCount())
                .featured(activity.getFeatured())
                .active(activity.getActive())
                .maxGroupSize(activity.getMaxGroupSize())
                .availableSlots(activity.getAvailableSlots())
                .imageUrl(activity.getImageUrl())
                .galleryImages(Collections.emptyList())
                .includedItems(Collections.emptyList())
                .excludedItems(Collections.emptyList())
                .itinerary(Collections.emptyList())
                .availableDates(Collections.emptyList())
                .departureLocation(translation.getDepartureLocation())
                .returnLocation(translation.getReturnLocation())
                .meetingTime(translation.getMeetingTime())
                .availability(translation.getAvailability())
                .whatToExpect(translation.getWhatToExpect())
                .complementaries(Collections.emptyList())
                .mapUrl(activity.getMapUrl())
                .destinationId(activity.getDestination() != null ? activity.getDestination().getId() : null)
                .destination(destinationMapper.toResponse(activity.getDestination(), languageCode))
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
