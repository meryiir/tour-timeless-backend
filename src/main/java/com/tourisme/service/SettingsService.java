package com.tourisme.service;

import com.tourisme.dto.request.SettingsTranslationRequest;
import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.entity.Settings;
import com.tourisme.entity.SettingsTranslation;
import com.tourisme.mapper.SettingsMapper;
import com.tourisme.repository.SettingsRepository;
import com.tourisme.repository.SettingsTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {
    
    private final SettingsRepository settingsRepository;
    private final SettingsTranslationRepository settingsTranslationRepository;
    private final SettingsMapper settingsMapper;
    
    public SettingsResponse getSettings() {
        return getSettings("en");
    }
    
    public SettingsResponse getSettings(String languageCode) {
        Settings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(Settings.builder().build());
        return settingsMapper.toResponse(settings, languageCode);
    }
    
    @Transactional
    public SettingsResponse updateSettings(String siteName, String logoUrl, String contactEmail,
                                          String contactPhone, String address, String facebookUrl,
                                          String instagramUrl, String twitterUrl, String youtubeUrl,
                                          String bannerTitle, String bannerSubtitle,
                                          List<SettingsTranslationRequest> translations) {
        Settings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(Settings.builder().build());
        
        // Update English (default) fields
        if (siteName != null) settings.setSiteName(siteName);
        if (logoUrl != null) settings.setLogoUrl(logoUrl);
        if (contactEmail != null) settings.setContactEmail(contactEmail);
        if (contactPhone != null) settings.setContactPhone(contactPhone);
        if (address != null) settings.setAddress(address);
        if (facebookUrl != null) settings.setFacebookUrl(facebookUrl);
        if (instagramUrl != null) settings.setInstagramUrl(instagramUrl);
        if (twitterUrl != null) settings.setTwitterUrl(twitterUrl);
        if (youtubeUrl != null) settings.setYoutubeUrl(youtubeUrl);
        if (bannerTitle != null) settings.setBannerTitle(bannerTitle);
        if (bannerSubtitle != null) settings.setBannerSubtitle(bannerSubtitle);
        
        settings = settingsRepository.save(settings);
        
        // Update translations
        if (translations != null && !translations.isEmpty()) {
            // Delete existing translations
            settingsTranslationRepository.deleteAll(settings.getTranslations());
            
            // Save new translations
            for (SettingsTranslationRequest translationRequest : translations) {
                SettingsTranslation translation = SettingsTranslation.builder()
                        .settings(settings)
                        .languageCode(translationRequest.getLanguageCode())
                        .siteName(translationRequest.getSiteName())
                        .bannerTitle(translationRequest.getBannerTitle())
                        .bannerSubtitle(translationRequest.getBannerSubtitle())
                        .address(translationRequest.getAddress())
                        .build();
                settingsTranslationRepository.save(translation);
            }
        }
        
        return settingsMapper.toResponse(settings);
    }
}
