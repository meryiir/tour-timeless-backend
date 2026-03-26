package com.tourisme.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DestinationPageCardResponse {
    private Long id;
    private Integer sortOrder;
    private String imageUrl;
    private String title;
    private String body;
    /** Present for admin edit responses only */
    private List<PageCardTranslationResponse> translations;
}
