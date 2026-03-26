package com.tourisme.repository;

import com.tourisme.entity.DestinationTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DestinationTranslationRepository extends JpaRepository<DestinationTranslation, Long> {
    Optional<DestinationTranslation> findByDestinationIdAndLanguageCode(Long destinationId, String languageCode);
}
