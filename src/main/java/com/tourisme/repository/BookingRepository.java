package com.tourisme.repository;

import com.tourisme.entity.Booking;
import com.tourisme.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    boolean existsByBookingReference(String bookingReference);
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
    Page<Booking> findByHiddenFalse(Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumTotalPriceByStatuses(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT b.status, COUNT(b) FROM Booking b GROUP BY b.status")
    List<Object[]> countBookingsGroupedByStatus();
}
