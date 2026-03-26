package com.tourisme.service;

import com.tourisme.dto.request.ReviewRequest;
import com.tourisme.dto.response.ReviewResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.Review;
import com.tourisme.entity.User;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.ReviewMapper;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.ReviewRepository;
import com.tourisme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ReviewMapper reviewMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
    
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User user = getCurrentUser();
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + request.getActivityId()));
        
        if (reviewRepository.findByUserIdAndActivityId(user.getId(), activity.getId()).isPresent()) {
            throw new BadRequestException("You have already reviewed this activity");
        }
        
        Review review = Review.builder()
                .user(user)
                .activity(activity)
                .rating(request.getRating())
                .comment(request.getComment())
                .approved(false)
                .build();
        
        review = reviewRepository.save(review);
        updateActivityRating(activity.getId());
        
        return reviewMapper.toResponse(review);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getActivityReviews(Long activityId, Pageable pageable) {
        return reviewRepository.findByActivityIdAndApprovedTrue(activityId, pageable)
                .map(reviewMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable)
                .map(reviewMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getRecentApprovedReviews(Pageable pageable, String languageCode) {
        return reviewRepository.findByApprovedTrueOrderByCreatedAtDesc(pageable)
                .map(r -> reviewMapper.toResponse(r, languageCode));
    }
    
    @Transactional
    public ReviewResponse approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        review.setApproved(true);
        review = reviewRepository.save(review);
        updateActivityRating(review.getActivity().getId());
        
        return reviewMapper.toResponse(review);
    }
    
    @Transactional
    public ReviewResponse rejectReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        review.setApproved(false);
        review = reviewRepository.save(review);
        updateActivityRating(review.getActivity().getId());
        
        return reviewMapper.toResponse(review);
    }
    
    @Transactional
    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));
        
        Long activityId = review.getActivity().getId();
        reviewRepository.delete(review);
        updateActivityRating(activityId);
    }
    
    @Transactional
    private void updateActivityRating(Long activityId) {
        Double averageRating = reviewRepository.findAverageRatingByActivityId(activityId);
        Long reviewCount = reviewRepository.countApprovedByActivityId(activityId);
        
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
        
        if (averageRating != null) {
            activity.setRatingAverage(BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP));
        } else {
            activity.setRatingAverage(BigDecimal.ZERO);
        }
        activity.setReviewCount(reviewCount.intValue());
        
        activityRepository.save(activity);
    }
}
