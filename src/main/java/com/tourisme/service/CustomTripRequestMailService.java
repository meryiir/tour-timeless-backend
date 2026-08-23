package com.tourisme.service;

import com.tourisme.entity.CustomTripRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

/**
 * Email notifications for custom trip planner submissions (admin inbox + visitor confirmation).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomTripRequestMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.booking.notification-to:tourinmorocco.contact@gmail.com}")
    private String bookingInbox;

    @Value("${app.contact.notification-to:}")
    private String contactInboxOverride;

    public boolean sendAdminNotificationEmail(CustomTripRequest request) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || !StringUtils.hasText(fromAddress)) {
            log.info("Custom trip id={}; admin email not sent (configure spring.mail.host and MAIL_USERNAME).",
                    request != null ? request.getId() : null);
            return false;
        }
        if (request == null) return false;

        String inbox = resolveInbox();
        if (!StringUtils.hasText(inbox)) {
            log.info("Custom trip id={}; no inbox address (set BOOKING_INBOX_EMAIL or CONTACT_INBOX_EMAIL).",
                    request.getId());
            return false;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setReplyTo(request.getEmail().trim());
            mail.setTo(inbox.trim().split("\\s*,\\s*"));
            mail.setSubject("[Custom trip] " + request.getStartCity() + " → " + request.getDestinationCity());
            mail.setText(buildAdminBody(request));
            sender.send(mail);
            log.info("Custom trip admin notification sent to {} for request id={}", inbox, request.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send custom trip admin notification for request id={}", request.getId(), e);
            return false;
        }
    }

    public boolean sendCustomerConfirmationEmail(CustomTripRequest request) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || !StringUtils.hasText(fromAddress)) {
            log.info("Custom trip id={}; customer confirmation not sent (configure spring.mail.host and MAIL_USERNAME).",
                    request != null ? request.getId() : null);
            return false;
        }
        if (request == null || !StringUtils.hasText(request.getEmail())) return false;

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setTo(request.getEmail().trim());
            mail.setSubject("We received your custom Morocco trip request");
            mail.setText(buildCustomerBody(request));
            sender.send(mail);
            log.info("Custom trip customer confirmation sent to {} for request id={}",
                    request.getEmail(), request.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send custom trip customer confirmation for request id={}", request.getId(), e);
            return false;
        }
    }

    private String resolveInbox() {
        if (StringUtils.hasText(bookingInbox)) return bookingInbox.trim();
        if (StringUtils.hasText(contactInboxOverride)) return contactInboxOverride.trim();
        return "";
    }

    private String buildAdminBody(CustomTripRequest r) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        sb.append("A new custom trip request was submitted on the website.\n\n");
        sb.append("Route: ").append(r.getStartCity()).append(" → ").append(r.getDestinationCity()).append("\n");
        if (r.getPreferredDate() != null) {
            sb.append("Preferred date: ").append(df.format(r.getPreferredDate())).append("\n");
        }
        if (r.getNumberOfPeople() != null) {
            sb.append("Guests: ").append(r.getNumberOfPeople()).append("\n");
        }
        sb.append("\nClient:\n");
        sb.append("- Name: ").append(r.getName()).append("\n");
        sb.append("- Email: ").append(r.getEmail()).append("\n");
        if (StringUtils.hasText(r.getPhone())) {
            sb.append("- Phone: ").append(r.getPhone().trim()).append("\n");
        }
        if (StringUtils.hasText(r.getMessage())) {
            sb.append("\nMessage:\n").append(r.getMessage().trim()).append("\n");
        }
        sb.append("\n---\n");
        sb.append("View it in Admin → Bookings → Custom trips.");
        return sb.toString();
    }

    private String buildCustomerBody(CustomTripRequest r) {
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        StringBuilder sb = new StringBuilder();
        sb.append("Dear ").append(r.getName()).append(",\n\n");
        sb.append("Thank you for your custom trip request. We have received your details and will contact you ");
        sb.append("within 24 hours to propose the best route, stays, and activities.\n\n");
        sb.append("Route: ").append(r.getStartCity()).append(" → ").append(r.getDestinationCity()).append("\n");
        if (r.getPreferredDate() != null) {
            sb.append("Preferred date: ").append(df.format(r.getPreferredDate())).append("\n");
        }
        if (r.getNumberOfPeople() != null) {
            sb.append("Guests: ").append(r.getNumberOfPeople()).append("\n");
        }
        sb.append("\nWarm regards,\n");
        sb.append("Mosaic Morocco Team");
        return sb.toString();
    }
}
