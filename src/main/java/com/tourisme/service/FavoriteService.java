package com.tourisme.service;

import com.tourisme.dto.response.FavoriteResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.Favorite;
import com.tourisme.entity.User;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.FavoriteMapper;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.FavoriteRepository;
import com.tourisme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final FavoriteMapper favoriteMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
    
    @Transactional
    public FavoriteResponse addFavorite(Long activityId) {
        User user = getCurrentUser();
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + activityId));
        
        if (favoriteRepository.existsByUserIdAndActivityId(user.getId(), activityId)) {
            throw new BadRequestException("Activity is already in favorites");
        }
        
        Favorite favorite = Favorite.builder()
                .user(user)
                .activity(activity)
                .build();
        
        favorite = favoriteRepository.save(favorite);
        return favoriteMapper.toResponse(favorite);
    }
    
    @Transactional
    public void removeFavorite(Long activityId) {
        User user = getCurrentUser();
        if (!favoriteRepository.existsByUserIdAndActivityId(user.getId(), activityId)) {
            throw new ResourceNotFoundException("Favorite not found");
        }
        favoriteRepository.deleteByUserIdAndActivityId(user.getId(), activityId);
    }
    
    public Page<FavoriteResponse> getMyFavorites(Pageable pageable) {
        User user = getCurrentUser();
        return favoriteRepository.findByUserId(user.getId(), pageable)
                .map(favoriteMapper::toResponse);
    }
}
