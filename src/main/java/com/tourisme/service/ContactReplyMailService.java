package com.tourisme.service;

import com.tourisme.entity.ContactMessage;
import com.tourisme.entity.Settings;
import com.tourisme.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends email for contact threads: admin reply to visitor, and new submissions to the inbox.
 * Requires {@link JavaMailSender} ({@code spring.mail.host}, {@code spring.mail.username}, etc.).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactReplyMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final SettingsRepository settingsRepository;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.contact.mail-from-name:Tour in Morocco}")
    private String fromName;

    /** Fallback if {@link Settings#getContactEmail()} is empty (e.g. env CONTACT_INBOX_EMAIL). */
    @Value("${app.contact.notification-to:}")
    private String notificationToOverride;

    /**
     * @return true if mail was sent, false if SMTP is not configured (reply still saved in DB).
     */
    public boolean sendReplyEmail(ContactMessage original, String replyBody) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("Contact reply saved for message id={}; email not sent (configure spring.mail.host and username).",
                    original.getId());
            return false;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setReplyTo(fromAddress);
            mail.setTo(original.getSenderEmail());
            mail.setSubject("Re: " + original.getSubject());
            mail.setText(buildReplyBody(original, replyBody));
            sender.send(mail);
            log.debug("Contact reply email sent to {} for message id={}", original.getSenderEmail(), original.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send contact reply email for message id={}", original.getId(), e);
            return false;
        }
    }

    /**
     * Notifies the site inbox that a new contact form message was received (in addition to DB + admin UI).
     *
     * @return true if sent, false if SMTP not configured or no recipient resolved
     */
    public boolean sendNewContactNotificationEmail(ContactMessage message) {
        JavaMailSender sender = javaMailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress == null || fromAddress.isBlank()) {
            log.info("New contact id={}; inbox email not sent (configure spring.mail.host and username).",
                    message.getId());
            return false;
        }

        String inbox = resolveInboxRecipient();
        if (!StringUtils.hasText(inbox)) {
            log.info("New contact id={}; no inbox address (set site contact email in Admin → Settings or CONTACT_INBOX_EMAIL / app.contact.notification-to).",
                    message.getId());
            return false;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromAddress);
            mail.setReplyTo(message.getSenderEmail());
            mail.setTo(inbox.trim().split("\\s*,\\s*"));
            mail.setSubject("[Contact form] " + message.getSubject());
            mail.setText(buildNewSubmissionBody(message));
            sender.send(mail);
            log.debug("New contact notification sent to {} for message id={}", inbox, message.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send new-contact notification email for message id={}", message.getId(), e);
            return false;
        }
    }

    private String resolveInboxRecipient() {
        return settingsRepository.findFirstByOrderByIdAsc()
                .map(Settings::getContactEmail)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(StringUtils.hasText(notificationToOverride) ? notificationToOverride.trim() : null);
    }

    private String buildReplyBody(ContactMessage original, String replyBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ").append(original.getSenderName()).append(",\n\n");
        sb.append(replyBody.trim());
        sb.append("\n\n---\n");
        sb.append(fromName).append("\n\n");
        sb.append("Your original message:\n");
        sb.append("Subject: ").append(original.getSubject()).append("\n\n");
        sb.append(original.getMessage());
        return sb.toString();
    }

    private String buildNewSubmissionBody(ContactMessage m) {
        StringBuilder sb = new StringBuilder();
        sb.append("You have a new message from the website contact form.\n\n");
        sb.append("From: ").append(m.getSenderName()).append(" <").append(m.getSenderEmail()).append(">\n");
        sb.append("Subject: ").append(m.getSubject()).append("\n\n");
        sb.append("Message:\n");
        sb.append(m.getMessage());
        sb.append("\n\n---\n");
        sb.append("Reply directly to this email to reach the visitor (Reply-To is set to their address).\n");
        sb.append("You can also manage this thread in Admin → Messages (ID ").append(m.getId()).append(").");
        return sb.toString();
    }
}
