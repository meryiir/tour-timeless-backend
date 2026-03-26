package com.tourisme.controller;

import com.tourisme.dto.request.ContactMessageRequest;
import com.tourisme.dto.response.ClientContactMessageResponse;
import com.tourisme.dto.response.ContactMessageResponse;
import com.tourisme.service.ContactMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactMessageService contactMessageService;

    @PostMapping("/messages")
    public ResponseEntity<ContactMessageResponse> submitMessage(
            @Valid @RequestBody ContactMessageRequest request,
            Authentication authentication) {
        ContactMessageResponse body = contactMessageService.submit(request, authentication);
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    @GetMapping("/my-messages")
    public ResponseEntity<Page<ClientContactMessageResponse>> myMessages(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(contactMessageService.findMyMessages(authentication, pageable));
    }
}
