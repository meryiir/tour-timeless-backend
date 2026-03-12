package com.tourisme.repository;

import com.tourisme.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByActivityIdAndApprovedTrue(Long activityId, Pageable pageable);
    Page<Review> findByApprovedFalse(Pageable pageable);
    Optional<Review> findByUserIdAndActivityId(Long userId, Long activityId);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.activity.id = :activityId AND r.approved = true")
    Double findAverageRatingByActivityId(@Param("activityId") Long activityId);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.activity.id = :activityId AND r.approved = true")
    Long countApprovedByActivityId(@Param("activityId") Long activityId);
}
