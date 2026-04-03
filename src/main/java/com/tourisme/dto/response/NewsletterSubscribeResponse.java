package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscribeResponse {
    private String email;
    /** True when this address was already on the active list (idempotent success). */
    private boolean alreadySubscribed;
    private LocalDateTime subscribedAt;
}
