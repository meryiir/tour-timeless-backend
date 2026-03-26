package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationTranslationSnippetResponse {
    private String languageCode;
    private String name;
    private String shortDescription;
    private String fullDescription;
}
