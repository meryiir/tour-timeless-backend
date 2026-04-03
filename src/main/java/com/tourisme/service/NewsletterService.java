package com.tourisme.service;

import com.tourisme.dto.request.NewsletterSubscribeRequest;
import com.tourisme.dto.response.NewsletterSubscribeResponse;
import com.tourisme.entity.NewsletterSubscriber;
import com.tourisme.repository.NewsletterSubscriberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Transactional
    public NewsletterSubscribeResponse subscribe(NewsletterSubscribeRequest request) {
        String email = normalizeEmail(request.getEmail());
        Optional<NewsletterSubscriber> existing = newsletterSubscriberRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            NewsletterSubscriber row = existing.get();
            if (Boolean.TRUE.equals(row.getActive())) {
                return NewsletterSubscribeResponse.builder()
                        .email(email)
                        .alreadySubscribed(true)
                        .subscribedAt(row.getCreatedAt())
                        .build();
            }
            row.setActive(true);
            newsletterSubscriberRepository.save(row);
            return NewsletterSubscribeResponse.builder()
                    .email(email)
                    .alreadySubscribed(false)
                    .subscribedAt(row.getCreatedAt())
                    .build();
        }
        NewsletterSubscriber saved = newsletterSubscriberRepository.save(
                NewsletterSubscriber.builder()
                        .email(email)
                        .active(true)
                        .build());
        return NewsletterSubscribeResponse.builder()
                .email(email)
                .alreadySubscribed(false)
                .subscribedAt(saved.getCreatedAt())
                .build();
    }

    private static String normalizeEmail(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
