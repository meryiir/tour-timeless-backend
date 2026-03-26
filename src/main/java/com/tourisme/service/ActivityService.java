package com.tourisme.service;

import com.tourisme.dto.request.ActivityRequest;
import com.tourisme.dto.request.ActivityTranslationRequest;
import com.tourisme.dto.response.ActivityResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.ActivityTranslation;
import com.tourisme.entity.Destination;
import com.tourisme.exception.DuplicateResourceException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.ActivityMapper;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.ActivityTranslationRepository;
import com.tourisme.repository.DestinationRepository;
import com.tourisme.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityMapper activityMapper;
    private final ActivityTranslationRepository activityTranslationRepository;
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getAllActivities(Pageable pageable) {
        return getAllActivities(pageable, "en");
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getAllActivities(Pageable pageable, String languageCode) {
        return activityRepository.findByActiveTrue(pageable)
                .map(activity -> {
                    // Initialize all lazy collections to avoid LazyInitializationException
                    Hibernate.initialize(activity.getGalleryImages());
                    Hibernate.initialize(activity.getIncludedItems());
                    Hibernate.initialize(activity.getExcludedItems());
                    Hibernate.initialize(activity.getItinerary());
                    Hibernate.initialize(activity.getAvailableDates());
                    Hibernate.initialize(activity.getComplementaries());
                    return activityMapper.toResponse(activity, languageCode);
                });
    }
    
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id) {
        return getActivityById(id, "en");
    }
    
    @Transactional(readOnly = true)
    public ActivityResponse getActivityById(Long id, String languageCode) {
        Activity activity = activityRepository.findByIdWithDestination(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        if (!activity.getActive()) {
            throw new ResourceNotFoundException("Activity not found with id: " + id);
        }
        // Initialize all lazy collections
        Hibernate.initialize(activity.getGalleryImages());
        Hibernate.initialize(activity.getIncludedItems());
        Hibernate.initialize(activity.getExcludedItems());
        Hibernate.initialize(activity.getItinerary());
        Hibernate.initialize(activity.getAvailableDates());
        Hibernate.initialize(activity.getComplementaries());
        return activityMapper.toResponse(activity, languageCode);
    }
    
    @Transactional(readOnly = true)
    public ActivityResponse getActivityBySlug(String slug) {
        return getActivityBySlug(slug, "en");
    }
    
    @Transactional(readOnly = true)
    public ActivityResponse getActivityBySlug(String slug, String languageCode) {
        Activity activity = activityRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with slug: " + slug));
        if (!activity.getActive()) {
            throw new ResourceNotFoundException("Activity not found with slug: " + slug);
        }
        // Initialize all lazy collections
        Hibernate.initialize(activity.getGalleryImages());
        Hibernate.initialize(activity.getIncludedItems());
        Hibernate.initialize(activity.getExcludedItems());
        Hibernate.initialize(activity.getItinerary());
        Hibernate.initialize(activity.getAvailableDates());
        Hibernate.initialize(activity.getComplementaries());
        return activityMapper.toResponse(activity, languageCode);
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getFeaturedActivities(Pageable pageable) {
        return getFeaturedActivities(pageable, "en");
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getFeaturedActivities(Pageable pageable, String languageCode) {
        return activityRepository.findByFeaturedTrueAndActiveTrue(pageable)
                .map(activity -> {
                    Hibernate.initialize(activity.getGalleryImages());
                    Hibernate.initialize(activity.getIncludedItems());
                    Hibernate.initialize(activity.getExcludedItems());
                    Hibernate.initialize(activity.getItinerary());
                    Hibernate.initialize(activity.getAvailableDates());
                    Hibernate.initialize(activity.getComplementaries());
                    return activityMapper.toResponse(activity, languageCode);
                });
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> searchActivities(String keyword, Pageable pageable) {
        return searchActivities(keyword, pageable, "en");
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> searchActivities(String keyword, Pageable pageable, String languageCode) {
        return activityRepository.search(keyword, pageable)
                .map(activity -> {
                    Hibernate.initialize(activity.getGalleryImages());
                    Hibernate.initialize(activity.getIncludedItems());
                    Hibernate.initialize(activity.getExcludedItems());
                    Hibernate.initialize(activity.getItinerary());
                    Hibernate.initialize(activity.getAvailableDates());
                    Hibernate.initialize(activity.getComplementaries());
                    return activityMapper.toResponse(activity, languageCode);
                });
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> filterActivities(
            Long destinationId, String category, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minRating, Activity.DifficultyLevel difficulty, Boolean featured,
            Pageable pageable) {
        return filterActivities(destinationId, category, minPrice, maxPrice, minRating, difficulty, featured, pageable, "en");
    }
    
    @Transactional(readOnly = true)
    public Page<ActivityResponse> filterActivities(
            Long destinationId, String category, BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minRating, Activity.DifficultyLevel difficulty, Boolean featured,
            Pageable pageable, String languageCode) {
        return activityRepository.findWithFilters(
                destinationId, category, minPrice, maxPrice, minRating, difficulty, featured, pageable)
                .map(activity -> {
                    Hibernate.initialize(activity.getGalleryImages());
                    Hibernate.initialize(activity.getIncludedItems());
                    Hibernate.initialize(activity.getExcludedItems());
                    Hibernate.initialize(activity.getItinerary());
                    Hibernate.initialize(activity.getAvailableDates());
                    Hibernate.initialize(activity.getComplementaries());
                    return activityMapper.toResponse(activity, languageCode);
                });
    }
    
    @Transactional
    public ActivityResponse createActivity(ActivityRequest request) {
        Destination destination = destinationRepository.findById(request.getDestinationId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + request.getDestinationId()));
        
        String slug = SlugUtil.generateSlug(request.getTitle());
        if (activityRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        
        Activity activity = Activity.builder()
                .title(request.getTitle())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .price(request.getPrice())
                .premiumPrice(request.getPremiumPrice())
                .budgetPrice(request.getBudgetPrice())
                .duration(request.getDuration())
                .location(request.getLocation())
                .category(request.getCategory())
                .difficultyLevel(request.getDifficultyLevel())
                .tourType(request.getTourType() != null ? request.getTourType() : Activity.TourType.SHARED)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .active(request.getActive() != null ? request.getActive() : true)
                .maxGroupSize(request.getMaxGroupSize() != null ? request.getMaxGroupSize() : 20)
                .availableSlots(request.getAvailableSlots() != null ? request.getAvailableSlots() : 50)
                .imageUrl(request.getImageUrl())
                .galleryImages(request.getGalleryImages() != null ? request.getGalleryImages() : new java.util.ArrayList<>())
                .includedItems(request.getIncludedItems() != null ? request.getIncludedItems() : new java.util.ArrayList<>())
                .excludedItems(request.getExcludedItems() != null ? request.getExcludedItems() : new java.util.ArrayList<>())
                .itinerary(request.getItinerary() != null ? request.getItinerary() : new java.util.ArrayList<>())
                .availableDates(request.getAvailableDates() != null ? request.getAvailableDates() : new java.util.ArrayList<>())
                .departureLocation(request.getDepartureLocation())
                .returnLocation(request.getReturnLocation())
                .meetingTime(request.getMeetingTime())
                .availability(request.getAvailability())
                .whatToExpect(request.getWhatToExpect())
                .complementaries(request.getComplementaries() != null ? request.getComplementaries() : new java.util.ArrayList<>())
                .mapUrl(request.getMapUrl())
                .destination(destination)
                .build();
        
        activity = activityRepository.save(activity);
        return activityMapper.toResponse(activity);
    }
    
    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest request) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        
        if (request.getTitle() != null && !request.getTitle().equals(activity.getTitle())) {
            String slug = SlugUtil.generateSlug(request.getTitle());
            if (activityRepository.existsBySlug(slug) && !slug.equals(activity.getSlug())) {
                slug = slug + "-" + System.currentTimeMillis();
            }
            activity.setTitle(request.getTitle());
            activity.setSlug(slug);
        }
        
        if (request.getDestinationId() != null) {
            Destination destination = destinationRepository.findById(request.getDestinationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + request.getDestinationId()));
            activity.setDestination(destination);
        }
        
        if (request.getShortDescription() != null) activity.setShortDescription(request.getShortDescription());
        if (request.getFullDescription() != null) activity.setFullDescription(request.getFullDescription());
        if (request.getPrice() != null) activity.setPrice(request.getPrice());
        if (request.getPremiumPrice() != null) activity.setPremiumPrice(request.getPremiumPrice());
        if (request.getBudgetPrice() != null) activity.setBudgetPrice(request.getBudgetPrice());
        if (request.getDuration() != null) activity.setDuration(request.getDuration());
        if (request.getLocation() != null) activity.setLocation(request.getLocation());
        if (request.getCategory() != null) activity.setCategory(request.getCategory());
        if (request.getDifficultyLevel() != null) activity.setDifficultyLevel(request.getDifficultyLevel());
        if (request.getTourType() != null) activity.setTourType(request.getTourType());
        if (request.getFeatured() != null) activity.setFeatured(request.getFeatured());
        if (request.getActive() != null) activity.setActive(request.getActive());
        if (request.getMaxGroupSize() != null) activity.setMaxGroupSize(request.getMaxGroupSize());
        if (request.getAvailableSlots() != null) activity.setAvailableSlots(request.getAvailableSlots());
        if (request.getImageUrl() != null) activity.setImageUrl(request.getImageUrl());
        if (request.getGalleryImages() != null) activity.setGalleryImages(request.getGalleryImages());
        if (request.getIncludedItems() != null) activity.setIncludedItems(request.getIncludedItems());
        if (request.getExcludedItems() != null) activity.setExcludedItems(request.getExcludedItems());
        if (request.getItinerary() != null) activity.setItinerary(request.getItinerary());
        if (request.getAvailableDates() != null) activity.setAvailableDates(request.getAvailableDates());
        if (request.getDepartureLocation() != null) activity.setDepartureLocation(request.getDepartureLocation());
        if (request.getReturnLocation() != null) activity.setReturnLocation(request.getReturnLocation());
        if (request.getMeetingTime() != null) activity.setMeetingTime(request.getMeetingTime());
        if (request.getAvailability() != null) activity.setAvailability(request.getAvailability());
        if (request.getWhatToExpect() != null) activity.setWhatToExpect(request.getWhatToExpect());
        if (request.getComplementaries() != null) activity.setComplementaries(request.getComplementaries());
        if (request.getMapUrl() != null) activity.setMapUrl(request.getMapUrl());
        
        activity = activityRepository.save(activity);
        
        // Update translations
        if (request.getTranslations() != null && !request.getTranslations().isEmpty()) {
            for (ActivityTranslationRequest translationRequest : request.getTranslations()) {
                // Check if translation already exists
                Optional<ActivityTranslation> existingTranslation = activityTranslationRepository
                        .findByActivityIdAndLanguageCode(activity.getId(), translationRequest.getLanguageCode());
                
                ActivityTranslation translation;
                if (existingTranslation.isPresent()) {
                    // Update existing translation
                    translation = existingTranslation.get();
                    translation.setTitle(translationRequest.getTitle());
                    translation.setShortDescription(translationRequest.getShortDescription());
                    translation.setFullDescription(translationRequest.getFullDescription());
                    translation.setLocation(translationRequest.getLocation());
                    translation.setCategory(translationRequest.getCategory());
                    translation.setDepartureLocation(translationRequest.getDepartureLocation());
                    translation.setReturnLocation(translationRequest.getReturnLocation());
                    translation.setMeetingTime(translationRequest.getMeetingTime());
                    translation.setAvailability(translationRequest.getAvailability());
                    translation.setWhatToExpect(translationRequest.getWhatToExpect());
                } else {
                    // Create new translation
                    translation = ActivityTranslation.builder()
                            .activity(activity)
                            .languageCode(translationRequest.getLanguageCode())
                            .title(translationRequest.getTitle())
                            .shortDescription(translationRequest.getShortDescription())
                            .fullDescription(translationRequest.getFullDescription())
                            .location(translationRequest.getLocation())
                            .category(translationRequest.getCategory())
                            .departureLocation(translationRequest.getDepartureLocation())
                            .returnLocation(translationRequest.getReturnLocation())
                            .meetingTime(translationRequest.getMeetingTime())
                            .availability(translationRequest.getAvailability())
                            .whatToExpect(translationRequest.getWhatToExpect())
                            .build();
                }
                activityTranslationRepository.save(translation);
            }
        }
        
        return activityMapper.toResponse(activity);
    }
    
    @Transactional
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Activity not found with id: " + id);
        }
        activityRepository.deleteById(id);
    }
    
    @Transactional
    public ActivityResponse updateActivityStatus(Long id, Boolean active) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        
        activity.setActive(active);
        activity = activityRepository.save(activity);
        return activityMapper.toResponse(activity);
    }
    
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return activityRepository.findDistinctCategories();
    }
    
    // Admin methods - don't filter by active status
    @Transactional(readOnly = true)
    public Page<ActivityResponse> getAllActivitiesForAdmin(Pageable pageable, String languageCode) {
        return activityRepository.findAll(pageable)
                .map(activity -> {
                    // Initialize all lazy collections to avoid LazyInitializationException
                    Hibernate.initialize(activity.getGalleryImages());
                    Hibernate.initialize(activity.getIncludedItems());
                    Hibernate.initialize(activity.getExcludedItems());
                    Hibernate.initialize(activity.getItinerary());
                    Hibernate.initialize(activity.getAvailableDates());
                    Hibernate.initialize(activity.getComplementaries());
                    return activityMapper.toResponse(activity, languageCode);
                });
    }
    
    @Transactional(readOnly = true)
    public ActivityResponse getActivityByIdForAdmin(Long id, String languageCode) {
        Activity activity = activityRepository.findByIdWithDestination(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + id));
        // Don't check active status for admin
        // Initialize all lazy collections
        Hibernate.initialize(activity.getGalleryImages());
        Hibernate.initialize(activity.getIncludedItems());
        Hibernate.initialize(activity.getExcludedItems());
        Hibernate.initialize(activity.getItinerary());
        Hibernate.initialize(activity.getAvailableDates());
        Hibernate.initialize(activity.getComplementaries());
        return activityMapper.toResponse(activity, languageCode);
    }
}
