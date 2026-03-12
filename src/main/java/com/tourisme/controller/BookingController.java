package com.tourisme.controller;

import com.tourisme.dto.request.BookingRequest;
import com.tourisme.dto.response.BookingResponse;
import com.tourisme.entity.Booking;
import com.tourisme.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    
    private final BookingService bookingService;
    
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getMyBookings(pageable));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
}
