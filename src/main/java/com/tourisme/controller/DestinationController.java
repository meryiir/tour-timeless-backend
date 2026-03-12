package com.tourisme.controller;

import com.tourisme.dto.response.DestinationResponse;
import com.tourisme.service.DestinationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/destinations")
@RequiredArgsConstructor
public class DestinationController {
    
    private final DestinationService destinationService;
    
    @GetMapping
    public ResponseEntity<Page<DestinationResponse>> getAllDestinations(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(destinationService.getAllDestinations(pageable));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponse> getDestinationById(@PathVariable Long id) {
        return ResponseEntity.ok(destinationService.getDestinationById(id));
    }
    
    @GetMapping("/slug/{slug}")
    public ResponseEntity<DestinationResponse> getDestinationBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(destinationService.getDestinationBySlug(slug));
    }
}
