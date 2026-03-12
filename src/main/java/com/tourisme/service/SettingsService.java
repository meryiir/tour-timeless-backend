package com.tourisme.service;

import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.entity.Settings;
import com.tourisme.mapper.SettingsMapper;
import com.tourisme.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SettingsService {
    
    private final SettingsRepository settingsRepository;
    private final SettingsMapper settingsMapper;
    
    public SettingsResponse getSettings() {
        Settings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(Settings.builder().build());
        return settingsMapper.toResponse(settings);
    }
    
    @Transactional
    public SettingsResponse updateSettings(String siteName, String logoUrl, String contactEmail,
                                          String contactPhone, String address, String facebookUrl,
                                          String instagramUrl, String twitterUrl, String youtubeUrl,
                                          String bannerTitle, String bannerSubtitle) {
        Settings settings = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(Settings.builder().build());
        
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
        return settingsMapper.toResponse(settings);
    }
}
