package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_user_notification_user", columnList = "user_id"),
        @Index(name = "idx_user_notification_read", columnList = "user_id,is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** BOOKING_STATUS (default) or CONTACT_REPLY */
    @Column(name = "notification_type", nullable = false, length = 30)
    @ColumnDefault("'BOOKING_STATUS'")
    @Builder.Default
    private String notificationType = "BOOKING_STATUS";

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_reference", length = 50)
    private String bookingReference;

    /** Booking: activity title. Contact reply: original message subject. */
    @Column(name = "activity_title", length = 255)
    private String activityTitle;

    /** Booking: {@link com.tourisme.entity.Booking.BookingStatus} name. Contact: literal CONTACT_REPLY. */
    @Column(length = 20)
    private String status;

    @Column(name = "contact_message_id")
    private Long contactMessageId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean viewed = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
