package com.tourisme.dto.response;

import com.tourisme.entity.Booking.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private UserResponse user;
    private ActivityResponse activity;
    private LocalDate bookingDate;
    private LocalDate travelDate;
    private Integer numberOfPeople;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private String specialRequest;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
