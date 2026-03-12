package com.tourisme.mapper;

import com.tourisme.dto.response.FavoriteResponse;
import com.tourisme.entity.Favorite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteMapper {
    
    private final ActivityMapper activityMapper;
    
    public FavoriteResponse toResponse(Favorite favorite) {
        if (favorite == null) {
            return null;
        }
        
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .activity(activityMapper.toResponse(favorite.getActivity()))
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
