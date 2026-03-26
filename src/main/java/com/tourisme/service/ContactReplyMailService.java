package com.tourisme.service;

import com.tourisme.entity.ContactMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends the admin reply to the visitor by email when {@link JavaMailSender} is available
 * ({@code spring.mail.host} set — e.g. Gmail SMTP with app password).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactReplyMailService {

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.contact.mail-from-name:Tour in Morocco}")
    private String fromName;

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
            mail.setText(buildBody(original, replyBody));
            sender.send(mail);
            log.debug("Contact reply email sent to {} for message id={}", original.getSenderEmail(), original.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send contact reply email for message id={}", original.getId(), e);
            return false;
        }
    }

    private String buildBody(ContactMessage original, String replyBody) {
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
}
