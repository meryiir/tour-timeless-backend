package com.tourisme.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterSubscribeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    @Size(max = 255)
    private String email;
}
