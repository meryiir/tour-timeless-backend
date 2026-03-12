package com.tourisme.mapper;

import com.tourisme.dto.response.ReviewResponse;
import com.tourisme.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewMapper {
    
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    
    public ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        
        return ReviewResponse.builder()
                .id(review.getId())
                .user(userMapper.toResponse(review.getUser()))
                .activity(activityMapper.toResponse(review.getActivity()))
                .rating(review.getRating())
                .comment(review.getComment())
                .approved(review.getApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
