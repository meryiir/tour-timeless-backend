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
            log.info("New booking notification sent to {} for booking id={}", inboxTrimmed, booking.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send new-booking notification email for booking id={}", booking.getId(), e);
            return false;
        }
    }

    /**
     * @return true if mail was sent, false if SMTP not configured or no recipient resolved.
     */
    public boolean sendCustomerBookingConfirmationEmail(Booking booking) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("Customer booking confirmation id={}; email not sent (configure spring.mail.host and username).",
                    booking != null ? booking.getId() : null);
            return false;
        }

        if (booking == null || booking.getUser() == null) return false;

        String customerEmail = booking.getUser().getEmail();
        if (!StringUtils.hasText(customerEmail)) {
            log.info("Customer booking confirmation id={}; no customer email on file.", booking.getId());
            return false;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(customerEmail.trim());
            mail.setSubject(buildCustomerSubject(booking));
            mail.setText(buildCustomerBody(booking));
            sender.send(mail);
            log.info("Customer booking confirmation sent to {} for booking id={}", customerEmail, booking.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send customer booking confirmation for booking id={}", booking.getId(), e);
            return false;
        }
    }

    private String buildCustomerSubject(Booking booking) {
        String ref = booking.getBookingReference() != null ? booking.getBookingReference() : ("#" + booking.getId());
        return "Booking request received — " + ref;
    }

    private String buildCustomerBody(Booking b) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;

        String activityTitle = b.getActivity() != null ? nullToEmpty(b.getActivity().getTitle()) : "";
        String clientName = b.getUser() != null ? (nullToEmpty(b.getUser().getFirstName()) + " " + nullToEmpty(b.getUser().getLastName())).trim() : "";
        String clientPhone = b.getUser() != null ? nullToEmpty(b.getUser().getPhone()) : "";

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(clientName)) {
            sb.append("Dear ").append(clientName).append(",\n\n");
        } else {
            sb.append("Dear traveler,\n\n");
        }

        sb.append("Thank you for your booking request. We have successfully received your reservation.\n");
        sb.append("Our team will review your booking and contact you within 24 hours to confirm all details.\n\n");

        sb.append("Booking reference: ").append(nullToEmpty(b.getBookingReference())).append("\n");
        sb.append("Activity: ").append(activityTitle).append("\n");
        sb.append("Travel date: ").append(b.getTravelDate() != null ? df.format(b.getTravelDate()) : "").append("\n");
        sb.append("Guests: ").append(b.getNumberOfPeople() != null ? b.getNumberOfPeople() : "").append("\n");
        sb.append("Tour type: ").append(formatTourType(b.getTourType())).append("\n");
        sb.append("Comfort level: ").append(formatComfortLevel(b.getComfortLevel())).append("\n");
        if (b.getTotalPrice() != null) {
            sb.append("Total price: ").append(b.getTotalPrice().toPlainString()).append("\n");
        }
        sb.append("\n");

        if (StringUtils.hasText(clientPhone)) {
            sb.append("Contact phone on file: ").append(clientPhone).append("\n\n");
        }

        if (StringUtils.hasText(b.getSpecialRequest())) {
            sb.append("Your special request:\n");
            sb.append(b.getSpecialRequest().trim()).append("\n\n");
        }

        sb.append("You can view or manage this booking anytime from your account on our website.\n\n");
        sb.append("Warm regards,\n");
        sb.append("Mosaic Morocco Team");
        return sb.toString();
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
        sb.append("Tour type: ").append(formatTourType(b.getTourType())).append("\n");
        sb.append("Comfort level: ").append(formatComfortLevel(b.getComfortLevel())).append("\n");
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

    private static String formatTourType(String tourType) {
        if (!StringUtils.hasText(tourType)) {
            return "Shared";
        }
        return switch (tourType.trim().toLowerCase()) {
            case "private" -> "Private";
            case "premium" -> "Premium";
            default -> "Shared";
        };
    }

    private static String formatComfortLevel(String comfortLevel) {
        if (!StringUtils.hasText(comfortLevel)) {
            return "Standard Comfort";
        }
        return "luxury".equalsIgnoreCase(comfortLevel.trim()) ? "Luxury Experience" : "Standard Comfort";
    }
}

