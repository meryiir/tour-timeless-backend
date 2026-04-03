package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsTranslationRowResponse {
    private String languageCode;
    private String siteName;
    private String bannerTitle;
    private String bannerSubtitle;
    private String address;
    private String businessHours;
    private String aboutContentJson;
}
