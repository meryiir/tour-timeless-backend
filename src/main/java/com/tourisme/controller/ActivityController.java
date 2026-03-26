package com.tourisme.controller;

import com.tourisme.dto.response.ActivityResponse;
import com.tourisme.entity.Activity;
import com.tourisme.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {
    
    private final ActivityService activityService;
    
    @GetMapping
    public ResponseEntity<Page<ActivityResponse>> getAllActivities(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getAllActivities(pageable, lang));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getActivityById(id, lang));
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ActivityResponse> getActivityBySlug(
            @PathVariable String slug,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getActivityBySlug(slug, lang));
    }
    
    @GetMapping("/featured")
    public ResponseEntity<Page<ActivityResponse>> getFeaturedActivities(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getFeaturedActivities(pageable, lang));
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<ActivityResponse>> searchActivities(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.searchActivities(keyword, pageable, lang));
    }
    
    @GetMapping("/filter")
    public ResponseEntity<Page<ActivityResponse>> filterActivities(
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) Activity.DifficultyLevel difficulty,
            @RequestParam(required = false) Boolean featured,
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.filterActivities(
                destinationId, category, minPrice, maxPrice, minRating, difficulty, featured, pageable, lang));
    }
    
    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(activityService.getAllCategories());
    }
}
