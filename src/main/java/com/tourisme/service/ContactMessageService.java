package com.tourisme.service;

import com.tourisme.dto.request.ContactMessageReplyRequest;
import com.tourisme.dto.request.ContactMessageRequest;
import com.tourisme.dto.response.ClientContactMessageResponse;
import com.tourisme.dto.response.ContactMessageResponse;
import com.tourisme.entity.ContactMessage;
import com.tourisme.entity.User;
import com.tourisme.entity.User.Role;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.repository.ContactMessageRepository;
import com.tourisme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ContactReplyMailService contactReplyMailService;
    private final UserRepository userRepository;
    private final UserNotificationService userNotificationService;

    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest request, Authentication authentication) {
        User clientUser = resolveSubmittingClient(authentication);
        ContactMessage m = ContactMessage.builder()
                .senderName(request.getName().trim())
                .senderEmail(request.getEmail().trim())
                .user(clientUser)
                .subject(request.getSubject().trim())
                .message(request.getMessage().trim())
                .readByAdmin(false)
                .build();
        contactMessageRepository.save(m);
        return toResponse(m);
    }

    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> findAllForAdmin(Pageable pageable) {
        return contactMessageRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ClientContactMessageResponse> findMyMessages(Authentication authentication, Pageable pageable) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getRole() != Role.ROLE_CLIENT) {
            throw new BadRequestException("Only clients have a contact inbox here");
        }
        return contactMessageRepository.findForClientInbox(user.getId(), user.getEmail(), pageable)
                .map(this::toClientResponse);
    }

    @Transactional
    public ContactMessageResponse markRead(Long id) {
        ContactMessage m = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
        m.setReadByAdmin(true);
        return toResponse(m);
    }

    @Transactional
    public void delete(Long id) {
        if (!contactMessageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Contact message not found");
        }
        contactMessageRepository.deleteById(id);
    }

    @Transactional
    public ContactMessageResponse reply(Long id, ContactMessageReplyRequest request) {
        ContactMessage m = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
        if (m.getRepliedAt() != null) {
            throw new BadRequestException("This inquiry was already answered.");
        }
        String text = request.getReply().trim();
        m.setAdminReply(text);
        m.setRepliedAt(LocalDateTime.now());
        m.setReadByAdmin(true);
        contactMessageRepository.save(m);
        boolean delivered = contactReplyMailService.sendReplyEmail(m, text);
        notifyClientAboutReply(m);
        return toResponse(m, delivered);
    }

    private void notifyClientAboutReply(ContactMessage m) {
        User recipient = m.getUser();
        if (recipient == null) {
            recipient = userRepository.findByEmail(m.getSenderEmail()).orElse(null);
        }
        if (recipient != null
                && recipient.getRole() == Role.ROLE_CLIENT
                && Boolean.TRUE.equals(recipient.getActive())) {
            userNotificationService.notifyContactReply(recipient, m);
        }
    }

    private User resolveSubmittingClient(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            return null;
        }
        boolean isClient = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CLIENT"::equals);
        if (!isClient) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }

    private ClientContactMessageResponse toClientResponse(ContactMessage m) {
        return ClientContactMessageResponse.builder()
                .id(m.getId())
                .subject(m.getSubject())
                .message(m.getMessage())
                .adminReply(m.getAdminReply())
                .repliedAt(m.getRepliedAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private ContactMessageResponse toResponse(ContactMessage m) {
        return toResponse(m, null);
    }

    private ContactMessageResponse toResponse(ContactMessage m, Boolean replyEmailDelivered) {
        return ContactMessageResponse.builder()
                .id(m.getId())
                .name(m.getSenderName())
                .email(m.getSenderEmail())
                .subject(m.getSubject())
                .message(m.getMessage())
                .readByAdmin(m.getReadByAdmin())
                .adminReply(m.getAdminReply())
                .repliedAt(m.getRepliedAt())
                .replyEmailDelivered(replyEmailDelivered)
                .createdAt(m.getCreatedAt())
                .build();
    }
}
