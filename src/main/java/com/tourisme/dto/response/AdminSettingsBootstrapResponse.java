package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Full settings row + all translation rows for the admin editor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettingsBootstrapResponse {
    private Long id;
    private String siteName;
    private String logoUrl;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String facebookUrl;
    private String instagramUrl;
    private String twitterUrl;
    private String youtubeUrl;
    private String bannerTitle;
    private String bannerSubtitle;
    private String mapEmbedUrl;
    private String contactPhonesJson;
    private String businessHours;
    private String aboutContentJson;
    private LocalDateTime updatedAt;
    private List<SettingsTranslationRowResponse> translations;
}
