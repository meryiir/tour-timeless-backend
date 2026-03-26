package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    /** BOOKING_STATUS or CONTACT_REPLY */
    private String notificationType;
    private Long bookingId;
    private String bookingReference;
    private String activityTitle;
    private String status;
    private Long contactMessageId;
    private boolean read;
    private LocalDateTime createdAt;
}
