package com.tourisme.repository;

import com.tourisme.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    Optional<Destination> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Destination> findByFeaturedTrue(Pageable pageable);
}
