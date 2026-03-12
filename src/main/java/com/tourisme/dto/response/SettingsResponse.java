package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsResponse {
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
    private LocalDateTime updatedAt;
}
