package com.tourisme.controller;

import com.tourisme.dto.request.ActivityRequest;
import com.tourisme.dto.request.ContactMessageReplyRequest;
import com.tourisme.dto.request.DestinationRequest;
import com.tourisme.dto.response.*;
import com.tourisme.entity.Booking;
import com.tourisme.service.ActivityService;
import com.tourisme.service.ContactMessageService;
import com.tourisme.service.BackupImportService;
import com.tourisme.service.BackupService;
import com.tourisme.service.BookingService;
import com.tourisme.service.DashboardService;
import com.tourisme.service.DestinationService;
import com.tourisme.service.FileStorageService;
import com.tourisme.service.PostgresDataDumpService;
import com.tourisme.service.ReviewService;
import com.tourisme.service.SettingsService;
import com.tourisme.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final BackupService backupService;
    private final BackupImportService backupImportService;
    private final PostgresDataDumpService postgresDataDumpService;
    private final ContactMessageService contactMessageService;
    
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
    @GetMapping("/destinations/{id}")
    public ResponseEntity<DestinationResponse> getDestinationForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationForAdmin(id));
    }

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
    @GetMapping("/activities")
    public ResponseEntity<Page<ActivityResponse>> getAllActivities(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getAllActivitiesForAdmin(pageable, lang));
    }
    
    @GetMapping("/activities/{id}")
    public ResponseEntity<ActivityResponse> getActivityById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(activityService.getActivityByIdForAdmin(id, lang));
    }
    
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
    
    @GetMapping("/users/{id}/bookings")
    public ResponseEntity<Page<BookingResponse>> getUserBookings(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(bookingService.getBookingsByUserId(id, pageable));
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
    
    // Contact messages (website form)
    @GetMapping("/contact-messages")
    public ResponseEntity<Page<ContactMessageResponse>> getContactMessages(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(contactMessageService.findAllForAdmin(pageable));
    }
    
    @PatchMapping("/contact-messages/{id}/read")
    public ResponseEntity<ContactMessageResponse> markContactMessageRead(@PathVariable Long id) {
        return ResponseEntity.ok(contactMessageService.markRead(id));
    }
    
    @DeleteMapping("/contact-messages/{id}")
    public ResponseEntity<Void> deleteContactMessage(@PathVariable Long id) {
        contactMessageService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/contact-messages/{id}/reply")
    public ResponseEntity<ContactMessageResponse> replyToContactMessage(
            @PathVariable Long id,
            @Valid @RequestBody ContactMessageReplyRequest request) {
        return ResponseEntity.ok(contactMessageService.reply(id, request));
    }
    
    // Settings
    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> getSettings(
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(settingsService.getSettings(lang));
    }
    
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
            @RequestParam(required = false) String bannerSubtitle,
            @RequestBody(required = false) java.util.List<com.tourisme.dto.request.SettingsTranslationRequest> translations) {
        return ResponseEntity.ok(settingsService.updateSettings(
                siteName, logoUrl, contactEmail, contactPhone, address,
                facebookUrl, instagramUrl, twitterUrl, youtubeUrl,
                bannerTitle, bannerSubtitle, translations));
    }
    
    // Dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }
    
    /** Full JSON snapshot (passwords redacted). Admin only. */
    @GetMapping("/backup/export")
    public ResponseEntity<byte[]> exportBackup() throws Exception {
        byte[] data = backupService.exportJsonPretty();
        String filename = "tourisme-backup-" + LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }
    
    /**
     * PostgreSQL data-only SQL (pg_dump). Requires {@code pg_dump} on the server host; configure {@code app.backup.pg-dump-path} if needed.
     */
    @GetMapping("/backup/postgres-data")
    public ResponseEntity<byte[]> exportPostgresData(
            @RequestParam(value = "useInserts", defaultValue = "false") boolean useInserts) {
        byte[] data = postgresDataDumpService.dumpDataOnlyPlainSql(useInserts);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));
        String filename = "tourisme-data-" + stamp + ".sql";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(data);
    }
    
    @PostMapping(value = "/backup/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BackupImportResultResponse> importBackup(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "replaceExisting", defaultValue = "true") boolean replaceExisting,
            @RequestParam(value = "defaultPassword", required = false) String defaultPassword) throws Exception {
        return ResponseEntity.ok(backupImportService.importFromJson(file, replaceExisting, defaultPassword));
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
