package com.tourisme.mapper;

import com.tourisme.dto.response.DestinationResponse;
import com.tourisme.entity.Destination;
import org.springframework.stereotype.Component;

@Component
public class DestinationMapper {
    
    public DestinationResponse toResponse(Destination destination) {
        if (destination == null) {
            return null;
        }
        
        return DestinationResponse.builder()
                .id(destination.getId())
                .name(destination.getName())
                .slug(destination.getSlug())
                .shortDescription(destination.getShortDescription())
                .fullDescription(destination.getFullDescription())
                .imageUrl(destination.getImageUrl())
                .country(destination.getCountry())
                .city(destination.getCity())
                .featured(destination.getFeatured())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .build();
    }
}
