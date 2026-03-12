package com.tourisme.mapper;

import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.entity.Settings;
import org.springframework.stereotype.Component;

@Component
public class SettingsMapper {
    
    public SettingsResponse toResponse(Settings settings) {
        if (settings == null) {
            return null;
        }
        
        return SettingsResponse.builder()
                .id(settings.getId())
                .siteName(settings.getSiteName())
                .logoUrl(settings.getLogoUrl())
                .contactEmail(settings.getContactEmail())
                .contactPhone(settings.getContactPhone())
                .address(settings.getAddress())
                .facebookUrl(settings.getFacebookUrl())
                .instagramUrl(settings.getInstagramUrl())
                .twitterUrl(settings.getTwitterUrl())
                .youtubeUrl(settings.getYoutubeUrl())
                .bannerTitle(settings.getBannerTitle())
                .bannerSubtitle(settings.getBannerSubtitle())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
