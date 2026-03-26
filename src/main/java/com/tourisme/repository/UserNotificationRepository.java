package com.tourisme.repository;

import com.tourisme.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    Page<UserNotification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUser_IdAndViewedIsFalse(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserNotification n SET n.viewed = true WHERE n.user.id = :userId AND n.viewed = false")
    int markAllReadForUser(@Param("userId") Long userId);
}
