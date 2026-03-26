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
        return toResponse(review, "en");
    }

    public ReviewResponse toResponse(Review review, String languageCode) {
        if (review == null) {
            return null;
        }

        String lang = languageCode == null || languageCode.isBlank() ? "en" : languageCode.trim();

        return ReviewResponse.builder()
                .id(review.getId())
                .user(userMapper.toResponse(review.getUser()))
                .activity(activityMapper.toSummaryResponse(review.getActivity(), lang))
                .rating(review.getRating())
                .comment(review.getComment())
                .approved(review.getApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
