package com.tourisme.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourisme.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final ObjectMapper objectMapper;

    @Value("${app.google.client-id:}")
    private String expectedClientId;

    public GoogleUserProfile fetchVerifiedProfile(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BadRequestException("Google access token is required");
        }

        RestTemplate restTemplate = new RestTemplate();

        if (expectedClientId != null && !expectedClientId.isBlank()) {
            URI tokenInfoUri = UriComponentsBuilder.fromUriString(TOKENINFO_URL)
                    .queryParam("access_token", accessToken)
                    .build()
                    .toUri();
            try {
                ResponseEntity<String> tokenInfo = restTemplate.getForEntity(tokenInfoUri, String.class);
                JsonNode ti = objectMapper.readTree(tokenInfo.getBody());
                String aud = ti.path("aud").asText(null);
                if (aud == null || !expectedClientId.equals(aud)) {
                    throw new BadRequestException("Invalid Google token for this application");
                }
            } catch (RestClientException e) {
                throw new BadRequestException("Could not validate Google token");
            } catch (Exception e) {
                throw new BadRequestException("Could not validate Google token");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    USERINFO_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            JsonNode body = objectMapper.readTree(response.getBody());
            String email = body.path("email").asText(null);
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Google account has no email");
            }
            // v3 userinfo uses "email_verified"; older payloads used "verified_email"
            boolean verified = body.path("email_verified").asBoolean(false)
                    || body.path("verified_email").asBoolean(false);
            if (!verified) {
                throw new BadRequestException("Google email is not verified");
            }
            String given = textOrNull(body, "given_name");
            String family = textOrNull(body, "family_name");
            return new GoogleUserProfile(email, given, family);
        } catch (BadRequestException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BadRequestException("Google sign-in failed: invalid or expired token");
        } catch (Exception e) {
            throw new BadRequestException("Google sign-in failed");
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        String v = node.path(field).asText(null);
        return v != null && !v.isBlank() ? v : null;
    }

    public record GoogleUserProfile(String email, String givenName, String familyName) {}
}
