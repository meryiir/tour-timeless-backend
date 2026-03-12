package com.tourisme.repository;

import com.tourisme.entity.Booking;
import com.tourisme.entity.Booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    boolean existsByBookingReference(String bookingReference);
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);
}
