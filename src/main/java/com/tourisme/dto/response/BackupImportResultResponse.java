package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupImportResultResponse {
    private int destinations;
    private int destinationTranslations;
    private int activities;
    private int activityTranslations;
    private int users;
    private int bookings;
    private int reviews;
    private int favorites;
    private int settingsRows;
    private int settingsTranslations;
    private String message;
}
