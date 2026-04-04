package com.tourisme.repository;

import com.tourisme.entity.Destination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    Optional<Destination> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<Destination> findByFeaturedTrue(Pageable pageable);

    @Query("SELECT d.slug FROM Destination d ORDER BY d.slug")
    List<String> findAllSlugs();
}
