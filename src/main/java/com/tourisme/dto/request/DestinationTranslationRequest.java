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
public class DestinationTranslationRequest {
    
    @Size(max = 10, message = "Language code must not exceed 10 characters")
    private String languageCode;
    
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;
    
    private String fullDescription;
}
