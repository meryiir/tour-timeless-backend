package com.tourisme.service;

import com.tourisme.entity.Activity;
import com.tourisme.entity.ActivityTranslation;
import com.tourisme.entity.Destination;
import com.tourisme.entity.DestinationPageCard;
import com.tourisme.entity.DestinationTranslation;
import com.tourisme.entity.Settings;
import com.tourisme.entity.SettingsTranslation;
import com.tourisme.repository.ActivityTranslationRepository;
import com.tourisme.repository.DestinationTranslationRepository;
import com.tourisme.repository.SettingsTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TranslationService {
    
    private final ActivityTranslationRepository activityTranslationRepository;
    private final DestinationTranslationRepository destinationTranslationRepository;
    private final SettingsTranslationRepository settingsTranslationRepository;
    
    @Transactional(readOnly = true)
    public ActivityTranslation getActivityTranslation(Activity activity, String languageCode) {
        if (languageCode == null || languageCode.equals("en")) {
            // Return default English from main entity
            return ActivityTranslation.builder()
                    .title(activity.getTitle())
                    .shortDescription(activity.getShortDescription())
                    .fullDescription(activity.getFullDescription())
                    .location(activity.getLocation())
                    .category(activity.getCategory())
                    .departureLocation(activity.getDepartureLocation())
                    .returnLocation(activity.getReturnLocation())
                    .meetingTime(activity.getMeetingTime())
                    .availability(activity.getAvailability())
                    .whatToExpect(activity.getWhatToExpect())
                    .build();
        }
        
        Optional<ActivityTranslation> translation = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), languageCode);
        
        if (translation.isPresent()) {
            ActivityTranslation tr = translation.get();
            // Fill missing fields with default English values
            if (tr.getTitle() == null) tr.setTitle(activity.getTitle());
            if (tr.getShortDescription() == null) tr.setShortDescription(activity.getShortDescription());
            if (tr.getFullDescription() == null) tr.setFullDescription(activity.getFullDescription());
            if (tr.getLocation() == null) tr.setLocation(activity.getLocation());
            if (tr.getCategory() == null) tr.setCategory(activity.getCategory());
            if (tr.getDepartureLocation() == null) tr.setDepartureLocation(activity.getDepartureLocation());
            if (tr.getReturnLocation() == null) tr.setReturnLocation(activity.getReturnLocation());
            if (tr.getMeetingTime() == null) tr.setMeetingTime(activity.getMeetingTime());
            if (tr.getAvailability() == null) tr.setAvailability(activity.getAvailability());
            if (tr.getWhatToExpect() == null) tr.setWhatToExpect(activity.getWhatToExpect());
            return tr;
        }
        
        // Fallback to English
        return getActivityTranslation(activity, "en");
    }
    
    @Transactional(readOnly = true)
    public DestinationTranslation getDestinationTranslation(Destination destination, String languageCode) {
        if (languageCode == null || languageCode.equals("en")) {
            // Return default English from main entity
            return DestinationTranslation.builder()
                    .name(destination.getName())
                    .shortDescription(destination.getShortDescription())
                    .fullDescription(destination.getFullDescription())
                    .build();
        }
        
        Optional<DestinationTranslation> translation = destinationTranslationRepository
                .findByDestinationIdAndLanguageCode(destination.getId(), languageCode);
        
        if (translation.isPresent()) {
            DestinationTranslation tr = translation.get();
            // Fill missing fields with default English values
            if (tr.getName() == null) tr.setName(destination.getName());
            if (tr.getShortDescription() == null) tr.setShortDescription(destination.getShortDescription());
            if (tr.getFullDescription() == null) tr.setFullDescription(destination.getFullDescription());
            return tr;
        }
        
        // Fallback to English
        return getDestinationTranslation(destination, "en");
    }

    @Transactional(readOnly = true)
    public ResolvedPageCard getPageCardTranslation(DestinationPageCard card, String languageCode) {
        if (languageCode == null || languageCode.equalsIgnoreCase("en")) {
            return new ResolvedPageCard(card.getTitle(), card.getBody());
        }
        if (card.getTranslations() == null || card.getTranslations().isEmpty()) {
            return new ResolvedPageCard(card.getTitle(), card.getBody());
        }
        return card.getTranslations().stream()
                .filter(t -> languageCode.equalsIgnoreCase(t.getLanguageCode()))
                .findFirst()
                .map(t -> new ResolvedPageCard(
                        nonBlankOr(t.getTitle(), card.getTitle()),
                        nonBlankOr(t.getBody(), card.getBody())))
                .orElse(new ResolvedPageCard(card.getTitle(), card.getBody()));
    }

    private static String nonBlankOr(String preferred, String fallback) {
        return (preferred != null && !preferred.isBlank()) ? preferred : fallback;
    }

    public record ResolvedPageCard(String title, String body) {}
    
    @Transactional(readOnly = true)
    public SettingsTranslation getSettingsTranslation(Settings settings, String languageCode) {
        if (languageCode == null || languageCode.equals("en")) {
            // Return default English from main entity
            return SettingsTranslation.builder()
                    .siteName(settings.getSiteName())
                    .bannerTitle(settings.getBannerTitle())
                    .bannerSubtitle(settings.getBannerSubtitle())
                    .address(settings.getAddress())
                    .businessHours(settings.getBusinessHours())
                    .aboutContentJson(settings.getAboutContentJson())
                    .build();
        }
        
        Optional<SettingsTranslation> translation = settingsTranslationRepository
                .findBySettingsIdAndLanguageCode(settings.getId(), languageCode);
        
        if (translation.isPresent()) {
            SettingsTranslation tr = translation.get();
            // Fill missing fields with default English values
            if (tr.getSiteName() == null) tr.setSiteName(settings.getSiteName());
            if (tr.getBannerTitle() == null) tr.setBannerTitle(settings.getBannerTitle());
            if (tr.getBannerSubtitle() == null) tr.setBannerSubtitle(settings.getBannerSubtitle());
            if (tr.getAddress() == null) tr.setAddress(settings.getAddress());
            if (tr.getBusinessHours() == null || tr.getBusinessHours().isBlank()) {
                tr.setBusinessHours(settings.getBusinessHours());
            }
            if (tr.getAboutContentJson() == null || tr.getAboutContentJson().isBlank()) {
                tr.setAboutContentJson(settings.getAboutContentJson());
            }
            return tr;
        }
        
        // Fallback to English
        return getSettingsTranslation(settings, "en");
    }
}
