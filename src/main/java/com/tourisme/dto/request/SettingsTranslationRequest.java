package com.tourisme.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsTranslationRequest {
    
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    @Size(max = 255, message = "Site name must not exceed 255 characters")
    private String siteName;
    
    @Size(max = 255, message = "Banner title must not exceed 255 characters")
    private String bannerTitle;
    
    @Size(max = 500, message = "Banner subtitle must not exceed 500 characters")
    private String bannerSubtitle;
    
    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;
}
