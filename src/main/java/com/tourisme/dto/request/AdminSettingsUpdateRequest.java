package com.tourisme.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full settings payload for {@code PUT /api/admin/settings} (JSON body).
 * Null fields are left unchanged; use empty string to clear text fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSettingsUpdateRequest {

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
    private List<SettingsTranslationRequest> translations;
}
