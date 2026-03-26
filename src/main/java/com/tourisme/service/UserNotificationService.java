package com.tourisme.service;

import com.tourisme.dto.response.NotificationResponse;
import com.tourisme.entity.Booking;
import com.tourisme.entity.ContactMessage;
import com.tourisme.entity.User;
import com.tourisme.entity.UserNotification;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.repository.UserNotificationRepository;
import com.tourisme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void notifyBookingStatus(User user, Booking booking, Booking.BookingStatus status) {
        UserNotification n = UserNotification.builder()
                .notificationType("BOOKING_STATUS")
                .user(user)
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .activityTitle(booking.getActivity().getTitle())
                .status(status.name())
                .contactMessageId(null)
                .viewed(false)
                .build();
        userNotificationRepository.save(n);
    }

    @Transactional
    public void notifyContactReply(User user, ContactMessage message) {
        UserNotification n = UserNotification.builder()
                .notificationType("CONTACT_REPLY")
                .user(user)
                .bookingId(null)
                .bookingReference(null)
                .activityTitle(message.getSubject())
                .status("CONTACT_REPLY")
                .contactMessageId(message.getId())
                .viewed(false)
                .build();
        userNotificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable) {
        User user = getCurrentUser();
        return userNotificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = getCurrentUser();
        return userNotificationRepository.countByUser_IdAndViewedIsFalse(user.getId());
    }

    @Transactional
    public void markRead(Long notificationId) {
        User user = getCurrentUser();
        UserNotification n = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot modify this notification");
        }
        n.setViewed(true);
        userNotificationRepository.save(n);
    }

    @Transactional
    public int markAllRead() {
        User user = getCurrentUser();
        return userNotificationRepository.markAllReadForUser(user.getId());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }

    private NotificationResponse toResponse(UserNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .notificationType(n.getNotificationType())
                .bookingId(n.getBookingId())
                .bookingReference(n.getBookingReference())
                .activityTitle(n.getActivityTitle())
                .status(n.getStatus())
                .contactMessageId(n.getContactMessageId())
                .read(n.isViewed())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
