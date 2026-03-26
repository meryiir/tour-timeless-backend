package com.tourisme.mapper;

import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.entity.Settings;
import com.tourisme.entity.SettingsTranslation;
import com.tourisme.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsMapper {
    
    private final TranslationService translationService;
    
    public SettingsResponse toResponse(Settings settings) {
        return toResponse(settings, "en");
    }
    
    public SettingsResponse toResponse(Settings settings, String languageCode) {
        if (settings == null) {
            return null;
        }
        
        SettingsTranslation translation = translationService.getSettingsTranslation(settings, languageCode);
        
        return SettingsResponse.builder()
                .id(settings.getId())
                .siteName(translation.getSiteName())
                .logoUrl(settings.getLogoUrl())
                .contactEmail(settings.getContactEmail())
                .contactPhone(settings.getContactPhone())
                .address(translation.getAddress())
                .facebookUrl(settings.getFacebookUrl())
                .instagramUrl(settings.getInstagramUrl())
                .twitterUrl(settings.getTwitterUrl())
                .youtubeUrl(settings.getYoutubeUrl())
                .bannerTitle(translation.getBannerTitle())
                .bannerSubtitle(translation.getBannerSubtitle())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
