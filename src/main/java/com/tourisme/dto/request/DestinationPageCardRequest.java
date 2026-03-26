package com.tourisme.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DestinationPageCardRequest {

    private Integer sortOrder;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 255)
    private String title;

    private String body;

    private List<DestinationPageCardTranslationRequest> translations;
}
