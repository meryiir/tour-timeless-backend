package com.tourisme.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String fullDescription;
    private String imageUrl;
    private String country;
    private String city;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Localized content blocks on the public destination page; omitted on list endpoints */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DestinationPageCardResponse> pageCards;

    /** Canonical EN + other languages for admin edit; omitted for public API */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DestinationTranslationSnippetResponse> destinationTranslations;
}
