package com.tourisme.service;

import com.tourisme.dto.request.AdminSettingsUpdateRequest;
import com.tourisme.dto.request.SettingsTranslationRequest;
import com.tourisme.dto.response.AdminSettingsBootstrapResponse;
import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.dto.response.SettingsTranslationRowResponse;
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

    @Transactional(readOnly = true)
    public AdminSettingsBootstrapResponse getSettingsBootstrap() {
        Settings s = settingsRepository.findFirstByOrderByIdAsc()
                .orElse(Settings.builder().build());
        List<SettingsTranslationRowResponse> rows = s.getTranslations().stream()
                .map(t -> SettingsTranslationRowResponse.builder()
                        .languageCode(t.getLanguageCode())
                        .siteName(t.getSiteName())
                        .bannerTitle(t.getBannerTitle())
                        .bannerSubtitle(t.getBannerSubtitle())
                        .address(t.getAddress())
                        .businessHours(t.getBusinessHours())
                        .aboutContentJson(t.getAboutContentJson())
                        .build())
                .toList();
        return AdminSettingsBootstrapResponse.builder()
                .id(s.getId())
                .siteName(s.getSiteName())
                .logoUrl(s.getLogoUrl())
                .contactEmail(s.getContactEmail())
                .contactPhone(s.getContactPhone())
                .address(s.getAddress())
                .facebookUrl(s.getFacebookUrl())
                .instagramUrl(s.getInstagramUrl())
                .twitterUrl(s.getTwitterUrl())
                .youtubeUrl(s.getYoutubeUrl())
                .bannerTitle(s.getBannerTitle())
                .bannerSubtitle(s.getBannerSubtitle())
                .mapEmbedUrl(s.getMapEmbedUrl())
                .contactPhonesJson(s.getContactPhonesJson())
                .businessHours(s.getBusinessHours())
                .aboutContentJson(s.getAboutContentJson())
                .updatedAt(s.getUpdatedAt())
                .translations(rows)
                .build();
    }

    @Transactional
    public SettingsResponse updateSettings(AdminSettingsUpdateRequest request) {
        return updateSettings(
                request.getSiteName(),
                request.getLogoUrl(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getAddress(),
                request.getFacebookUrl(),
                request.getInstagramUrl(),
                request.getTwitterUrl(),
                request.getYoutubeUrl(),
                request.getBannerTitle(),
                request.getBannerSubtitle(),
                request.getMapEmbedUrl(),
                request.getContactPhonesJson(),
                request.getBusinessHours(),
                request.getAboutContentJson(),
                request.getTranslations());
    }

    @Transactional
    public SettingsResponse updateSettings(String siteName, String logoUrl, String contactEmail,
                                          String contactPhone, String address, String facebookUrl,
                                          String instagramUrl, String twitterUrl, String youtubeUrl,
                                          String bannerTitle, String bannerSubtitle,
                                          String mapEmbedUrl, String contactPhonesJson,
                                          String businessHours, String aboutContentJson,
                                          List<SettingsTranslationRequest> translations) {
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
        if (mapEmbedUrl != null) settings.setMapEmbedUrl(mapEmbedUrl);
        if (contactPhonesJson != null) settings.setContactPhonesJson(contactPhonesJson);
        if (businessHours != null) settings.setBusinessHours(businessHours);
        if (aboutContentJson != null) settings.setAboutContentJson(aboutContentJson);

        settings = settingsRepository.save(settings);

        if (translations != null && !translations.isEmpty()) {
            settingsTranslationRepository.deleteAll(settings.getTranslations());

            for (SettingsTranslationRequest translationRequest : translations) {
                SettingsTranslation translation = SettingsTranslation.builder()
                        .settings(settings)
                        .languageCode(translationRequest.getLanguageCode())
                        .siteName(translationRequest.getSiteName())
                        .bannerTitle(translationRequest.getBannerTitle())
                        .bannerSubtitle(translationRequest.getBannerSubtitle())
                        .address(translationRequest.getAddress())
                        .businessHours(translationRequest.getBusinessHours())
                        .aboutContentJson(translationRequest.getAboutContentJson())
                        .build();
                settingsTranslationRepository.save(translation);
            }
        }

        return settingsMapper.toResponse(settings);
    }
}
