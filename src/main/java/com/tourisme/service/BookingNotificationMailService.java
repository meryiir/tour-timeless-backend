package com.tourisme.service;

import com.tourisme.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Sends email notifications to the business inbox when a new booking is created.
 * Requires {@link JavaMailSender} ({@code spring.mail.host}, {@code spring.mail.username}, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingNotificationMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    /**
     * Business inbox for new bookings. Defaults to the requested address.
     * Override via env {@code BOOKING_INBOX_EMAIL} or property {@code app.booking.notification-to}.
     */
    @Value("${app.booking.notification-to:tourinmorocco.contact@gmail.com}")
    private String notificationTo;

    /**
     * @return true if mail was sent, false if SMTP not configured or no recipient resolved.
     */
    public boolean sendNewBookingNotificationEmail(Booking booking) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("New booking id={}; email not sent (configure spring.mail.host and username).",
                    booking != null ? booking.getId() : null);
            return false;
        }

        if (booking == null) return false;

        String inboxTrimmed = StringUtils.hasText(notificationTo) ? notificationTo.trim() : "";
        if (!StringUtils.hasText(inboxTrimmed)) {
            log.info("New booking id={}; no booking inbox address (set BOOKING_INBOX_EMAIL / app.booking.notification-to).",
                    booking.getId());
            return false;
        }

        try {
            String[] recipients = inboxTrimmed.split("\\s*,\\s*");
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            if (booking.getUser() != null && StringUtils.hasText(booking.getUser().getEmail())) {
                // Lets the admin hit "Reply" to reach the client.
                mail.setReplyTo(booking.getUser().getEmail());
            }
            mail.setTo(recipients);
            mail.setSubject(buildSubject(booking));
            mail.setText(buildBody(booking));
            sender.send(mail);
            log.debug("New booking notification sent to {} for booking id={}", inboxTrimmed, booking.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send new-booking notification email for booking id={}", booking.getId(), e);
            return false;
        }
    }

    private String buildSubject(Booking booking) {
        String ref = booking.getBookingReference() != null ? booking.getBookingReference() : ("#" + booking.getId());
        return "[New booking] " + ref;
    }

    private String buildBody(Booking b) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;

        String activityTitle = b.getActivity() != null ? nullToEmpty(b.getActivity().getTitle()) : "";
        String clientName = b.getUser() != null ? (nullToEmpty(b.getUser().getFirstName()) + " " + nullToEmpty(b.getUser().getLastName())).trim() : "";
        String clientEmail = b.getUser() != null ? nullToEmpty(b.getUser().getEmail()) : "";
        String clientPhone = b.getUser() != null ? nullToEmpty(b.getUser().getPhone()) : "";
        BigDecimal total = b.getTotalPrice();

        StringBuilder sb = new StringBuilder();
        sb.append("A new booking was created on the website.\n\n");

        sb.append("Reference: ").append(nullToEmpty(b.getBookingReference())).append("\n");
        sb.append("Status: ").append(b.getStatus() != null ? b.getStatus().name() : "").append("\n");
        sb.append("Activity: ").append(activityTitle).append("\n");
        sb.append("Travel date: ").append(b.getTravelDate() != null ? df.format(b.getTravelDate()) : "").append("\n");
        sb.append("People: ").append(b.getNumberOfPeople() != null ? b.getNumberOfPeople() : "").append("\n");
        sb.append("Total price: ").append(total != null ? total.toPlainString() : "").append("\n\n");

        sb.append("Client:\n");
        sb.append("- Name: ").append(clientName).append("\n");
        sb.append("- Email: ").append(clientEmail).append("\n");
        if (StringUtils.hasText(clientPhone)) {
            sb.append("- Phone: ").append(clientPhone).append("\n");
        }

        if (StringUtils.hasText(b.getSpecialRequest())) {
            sb.append("\nSpecial request:\n");
            sb.append(b.getSpecialRequest().trim()).append("\n");
        }

        sb.append("\n---\n");
        sb.append("You can also view/manage it in the Admin panel.");
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

