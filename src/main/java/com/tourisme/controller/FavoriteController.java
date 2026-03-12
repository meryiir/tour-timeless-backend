package com.tourisme.controller;

import com.tourisme.dto.response.FavoriteResponse;
import com.tourisme.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    
    private final FavoriteService favoriteService;
    
    @PostMapping("/{activityId}")
    public ResponseEntity<FavoriteResponse> addFavorite(@PathVariable Long activityId) {
        FavoriteResponse response = favoriteService.addFavorite(activityId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long activityId) {
        favoriteService.removeFavorite(activityId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/my-favorites")
    public ResponseEntity<Page<FavoriteResponse>> getMyFavorites(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(pageable));
    }
}
