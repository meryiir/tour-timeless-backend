package com.tourisme.repository;

import com.tourisme.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @EntityGraph(attributePaths = {"destination"})
    Optional<Activity> findBySlug(String slug);
    
    @EntityGraph(attributePaths = {"destination"})
    @Query("SELECT a FROM Activity a WHERE a.id = :id")
    Optional<Activity> findByIdWithDestination(@Param("id") Long id);
    boolean existsBySlug(String slug);
    
    @EntityGraph(attributePaths = {"destination"})
    Page<Activity> findByFeaturedTrueAndActiveTrueOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);
    
    @EntityGraph(attributePaths = {"destination"})
    Page<Activity> findByActiveTrueOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);
    Page<Activity> findByDestinationId(Long destinationId, Pageable pageable);
    Page<Activity> findByCategory(String category, Pageable pageable);
    
    @EntityGraph(attributePaths = {"destination"})
    @Query("SELECT a FROM Activity a WHERE a.active = true " +
           "AND (:destinationId IS NULL OR a.destination.id = :destinationId) " +
           "AND (:category IS NULL OR a.category = :category) " +
           "AND (:minPrice IS NULL OR a.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR a.price <= :maxPrice) " +
           "AND (:minRating IS NULL OR a.ratingAverage >= :minRating) " +
           "AND (:difficulty IS NULL OR a.difficultyLevel = :difficulty) " +
           "AND (:featured IS NULL OR a.featured = :featured) " +
           "ORDER BY a.displayOrder ASC, a.createdAt DESC")
    Page<Activity> findWithFilters(
        @Param("destinationId") Long destinationId,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minRating") BigDecimal minRating,
        @Param("difficulty") Activity.DifficultyLevel difficulty,
        @Param("featured") Boolean featured,
        Pageable pageable
    );
    
    @EntityGraph(attributePaths = {"destination"})
    @Query("SELECT a FROM Activity a WHERE a.active = true " +
           "AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.shortDescription) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(a.fullDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.displayOrder ASC, a.createdAt DESC")
    Page<Activity> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"destination"})
    Page<Activity> findAllByOrderByDisplayOrderAscCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT DISTINCT a.category FROM Activity a WHERE a.category IS NOT NULL AND a.category != '' ORDER BY a.category")
    List<String> findDistinctCategories();

    @Query("SELECT a.category, COUNT(a) FROM Activity a WHERE a.category IS NOT NULL AND a.category <> '' GROUP BY a.category")
    List<Object[]> countActivitiesGroupedByCategory();

    @Query("SELECT a.slug FROM Activity a WHERE a.active = true ORDER BY a.slug")
    List<String> findAllActiveSlugs();
}
