package com.tourisme.repository;

import com.tourisme.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndActivityId(Long userId, Long activityId);
    boolean existsByUserIdAndActivityId(Long userId, Long activityId);
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
    void deleteByUserIdAndActivityId(Long userId, Long activityId);
}
