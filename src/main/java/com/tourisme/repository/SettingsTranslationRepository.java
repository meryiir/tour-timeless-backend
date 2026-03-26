package com.tourisme.repository;

import com.tourisme.entity.SettingsTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SettingsTranslationRepository extends JpaRepository<SettingsTranslation, Long> {
    Optional<SettingsTranslation> findBySettingsIdAndLanguageCode(Long settingsId, String languageCode);
}
