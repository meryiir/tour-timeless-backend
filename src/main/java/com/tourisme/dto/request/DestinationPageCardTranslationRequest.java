package com.tourisme.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DestinationPageCardTranslationRequest {

    @NotBlank(message = "Card translation language code is required")
    @Size(max = 10)
    private String languageCode;

    @Size(max = 255)
    private String title;

    private String body;
}
