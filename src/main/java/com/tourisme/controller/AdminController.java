package com.tourisme.controller;

import com.tourisme.dto.request.ActivityRequest;
import com.tourisme.dto.request.DestinationRequest;
import com.tourisme.dto.response.*;
import com.tourisme.entity.Booking;
import com.tourisme.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final UserService userService;
    private final DestinationService destinationService;
    private final ActivityService activityService;
    private final BookingService bookingService;
    private final ReviewService reviewService;
    private final SettingsService settingsService;
    private final DashboardService dashboardService;
    private final FileStorageService fileStorageService;
    
    // Users
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone) {
        return ResponseEntity.ok(userService.updateUser(id, firstName, lastName, phone));
    }
    
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(userService.updateUserStatus(id, active));
    }
    
    // Destinations
    @PostMapping("/destinations")
    public ResponseEntity<DestinationResponse> createDestination(
            @Valid @RequestBody DestinationRequest request) {
        DestinationResponse response = destinationService.createDestination(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/destinations/{id}")
    public ResponseEntity<DestinationResponse> updateDestination(
            @PathVariable Long id,
            @Valid @RequestBody DestinationRequest request) {
        return ResponseEntity.ok(destinationService.updateDestination(id, request));
    }
    
    @DeleteMapping("/destinations/{id}")
    public ResponseEntity<Void> deleteDestination(@PathVariable Long id) {
        destinationService.deleteDestination(id);
        return ResponseEntity.noContent().build();
    }
    
    // Activities
    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> createActivity(
            @Valid @RequestBody ActivityRequest request) {
        ActivityResponse response = activityService.createActivity(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/activities/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody ActivityRequest request) {
        return ResponseEntity.ok(activityService.updateActivity(id, request));
    }
    
    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/activities/{id}/status")
    public ResponseEntity<ActivityResponse> updateActivityStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        return ResponseEntity.ok(activityService.updateActivityStatus(id, active));
    }
    
    // Bookings
    @GetMapping("/bookings")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getAllBookings(pageable));
    }
    
    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }
    
    @PatchMapping("/bookings/{id}/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam Booking.BookingStatus status) {
        return ResponseEntity.ok(bookingService.updateBookingStatus(id, status));
    }
    
    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
    
    // Reviews
    @GetMapping("/reviews")
    public ResponseEntity<Page<ReviewResponse>> getAllReviews(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reviewService.getAllReviews(pageable));
    }
    
    @PatchMapping("/reviews/{id}/approve")
    public ResponseEntity<ReviewResponse> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }
    
    @PatchMapping("/reviews/{id}/reject")
    public ResponseEntity<ReviewResponse> rejectReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }
    
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
    
    // Settings
    @PutMapping("/settings")
    public ResponseEntity<SettingsResponse> updateSettings(
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(required = false) String contactEmail,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String facebookUrl,
            @RequestParam(required = false) String instagramUrl,
            @RequestParam(required = false) String twitterUrl,
            @RequestParam(required = false) String youtubeUrl,
            @RequestParam(required = false) String bannerTitle,
            @RequestParam(required = false) String bannerSubtitle) {
        return ResponseEntity.ok(settingsService.updateSettings(
                siteName, logoUrl, contactEmail, contactPhone, address,
                facebookUrl, instagramUrl, twitterUrl, youtubeUrl,
                bannerTitle, bannerSubtitle));
    }
    
    // Dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
    
    // File Upload
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "File is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            String filename = fileStorageService.storeFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("url", "/uploads/" + filename);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload file: " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
