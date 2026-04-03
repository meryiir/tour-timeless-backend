package com.tourisme.controller;

import com.tourisme.dto.response.MapEmbedUrlResponse;
import com.tourisme.service.GoogleMapsEmbedResolverService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapEmbedController {

    private final GoogleMapsEmbedResolverService googleMapsEmbedResolverService;

    /**
     * Follows Google short links and builds a simple {@code output=embed} URL when lat/lng
     * can be read from the final Maps URL (viewport / place / data parameters).
     */
    @GetMapping("/embed-url")
    public ResponseEntity<MapEmbedUrlResponse> embedUrl(@RequestParam String url) {
        return ResponseEntity.ok(googleMapsEmbedResolverService.resolve(url));
    }
}
