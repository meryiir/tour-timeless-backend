package com.tourisme.repository;

import com.tourisme.entity.ActivityTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityTranslationRepository extends JpaRepository<ActivityTranslation, Long> {
    Optional<ActivityTranslation> findByActivityIdAndLanguageCode(Long activityId, String languageCode);
}
