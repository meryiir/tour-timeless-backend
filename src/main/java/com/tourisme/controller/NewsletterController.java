package com.tourisme.controller;

import com.tourisme.dto.request.NewsletterSubscribeRequest;
import com.tourisme.dto.response.NewsletterSubscribeResponse;
import com.tourisme.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<NewsletterSubscribeResponse> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        NewsletterSubscribeResponse body = newsletterService.subscribe(request);
        HttpStatus status = body.isAlreadySubscribed() ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(body, status);
    }
}
