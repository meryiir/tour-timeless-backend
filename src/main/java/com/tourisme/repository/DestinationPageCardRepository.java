package com.tourisme.repository;

import com.tourisme.entity.DestinationPageCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DestinationPageCardRepository extends JpaRepository<DestinationPageCard, Long> {

    /**
     * Plain select (no JOIN FETCH) so Hibernate always returns rows; translations load lazily
     * inside the same @Transactional service call when the mapper reads them.
     */
    @Query("SELECT c FROM DestinationPageCard c WHERE c.destination.id = :destId ORDER BY c.sortOrder ASC")
    List<DestinationPageCard> findByDestinationIdOrderBySortOrderAsc(@Param("destId") Long destId);
}
