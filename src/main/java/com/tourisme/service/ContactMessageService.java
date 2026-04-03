package com.tourisme.service;

import com.tourisme.dto.request.ContactMessageReplyRequest;
import com.tourisme.dto.request.ContactMessageRequest;
import com.tourisme.dto.request.ContactThreadMessageRequest;
import com.tourisme.dto.response.ClientContactMessageResponse;
import com.tourisme.dto.response.ContactMessageResponse;
import com.tourisme.dto.response.ContactThreadMessageResponse;
import com.tourisme.entity.ContactMessage;
import com.tourisme.entity.ContactMessageEntry;
import com.tourisme.entity.ContactMessageEntry.Sender;
import com.tourisme.entity.User;
import com.tourisme.entity.User.Role;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.repository.ContactMessageEntryRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageService {

    private final ContactMessageRepository contactMessageRepository;
    private final ContactMessageEntryRepository contactMessageEntryRepository;
    private final ContactReplyMailService contactReplyMailService;
    private final UserRepository userRepository;
    private final UserNotificationService userNotificationService;

    @Transactional
    public ContactMessageResponse submit(ContactMessageRequest request, Authentication authentication) {
        User clientUser = resolveSubmittingClient(authentication);
        String text = request.getMessage().trim();
        ContactMessage m = ContactMessage.builder()
                .senderName(request.getName().trim())
                .senderEmail(request.getEmail().trim())
                .user(clientUser)
                .subject(request.getSubject().trim())
                // Keep legacy column populated (thread entries are source of truth now).
                .message(text)
                .readByAdmin(false)
                .build();
        contactMessageRepository.save(m);
        contactMessageEntryRepository.save(ContactMessageEntry.builder()
                .contactMessage(m)
                .sender(Sender.CLIENT)
                .body(text)
                .build());
        notifyAdminsAboutNewClientMessage(m);
        contactReplyMailService.sendNewContactNotificationEmail(m);
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
        String text = request.getReply().trim();
        LocalDateTime now = LocalDateTime.now();
        contactMessageEntryRepository.save(ContactMessageEntry.builder()
                .contactMessage(m)
                .sender(Sender.ADMIN)
                .body(text)
                .createdAt(now)
                .build());
        // Legacy fields remain for backward compatibility (last admin reply only).
        m.setAdminReply(text);
        m.setRepliedAt(now);
        m.setReadByAdmin(true);
        contactMessageRepository.save(m);
        boolean delivered = contactReplyMailService.sendReplyEmail(m, text);
        notifyClientAboutReply(m);
        return toResponse(m, delivered);
    }

    @Transactional
    public ClientContactMessageResponse postClientThreadMessage(Long threadId, ContactThreadMessageRequest request, Authentication authentication) {
        User user = requireClient(authentication);
        ContactMessage thread = contactMessageRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found"));
        // Only allow posting into own thread (or guest thread matching email).
        boolean isOwner = (thread.getUser() != null && thread.getUser().getId().equals(user.getId()))
                || (thread.getUser() == null && thread.getSenderEmail() != null
                    && thread.getSenderEmail().trim().equalsIgnoreCase(user.getEmail()));
        if (!isOwner) {
            throw new BadRequestException("You cannot post in this thread");
        }
        String text = request.getMessage().trim();
        contactMessageEntryRepository.save(ContactMessageEntry.builder()
                .contactMessage(thread)
                .sender(Sender.CLIENT)
                .body(text)
                .build());
        // Keep legacy field updated to the latest client message for admin list preview.
        thread.setMessage(text);
        thread.setReadByAdmin(false);
        contactMessageRepository.save(thread);
        notifyAdminsAboutNewClientMessage(thread);
        return toClientResponse(thread);
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

    private void notifyAdminsAboutNewClientMessage(ContactMessage thread) {
        userRepository.findByRoleAndActiveIsTrue(Role.ROLE_ADMIN)
                .forEach((admin) -> userNotificationService.notifyAdminContactMessage(admin, thread));
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
        List<ContactThreadMessageResponse> thread = contactMessageEntryRepository
                .findByContactMessage_IdOrderByCreatedAtAsc(m.getId())
                .stream()
                .map(this::toThreadMessage)
                .toList();
        return ClientContactMessageResponse.builder()
                .id(m.getId())
                .subject(m.getSubject())
                .message(m.getMessage())
                .adminReply(m.getAdminReply())
                .repliedAt(m.getRepliedAt())
                .createdAt(m.getCreatedAt())
                .thread(thread)
                .build();
    }

    private ContactMessageResponse toResponse(ContactMessage m) {
        return toResponse(m, null);
    }

    private ContactMessageResponse toResponse(ContactMessage m, Boolean replyEmailDelivered) {
        List<ContactThreadMessageResponse> thread = contactMessageEntryRepository
                .findByContactMessage_IdOrderByCreatedAtAsc(m.getId())
                .stream()
                .map(this::toThreadMessage)
                .toList();
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
                .thread(thread)
                .build();
    }

    private ContactThreadMessageResponse toThreadMessage(ContactMessageEntry e) {
        return ContactThreadMessageResponse.builder()
                .id(e.getId())
                .sender(e.getSender() != null ? e.getSender().name() : null)
                .body(e.getBody())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private User requireClient(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (user.getRole() != Role.ROLE_CLIENT) {
            throw new BadRequestException("Only clients can send messages here");
        }
        return user;
    }
}
