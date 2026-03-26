package com.tourisme.controller;

import com.tourisme.dto.request.ReviewRequest;
import com.tourisme.dto.response.ReviewResponse;
import com.tourisme.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/activity/{activityId}")
    public ResponseEntity<Page<ReviewResponse>> getActivityReviews(
            @PathVariable Long activityId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getActivityReviews(activityId, pageable));
    }

    @GetMapping("/recent")
    public ResponseEntity<Page<ReviewResponse>> getRecentReviews(
            @PageableDefault(size = 9) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(reviewService.getRecentApprovedReviews(pageable, lang));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
