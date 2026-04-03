package com.tourisme.controller;

import com.tourisme.dto.response.SettingsResponse;
import com.tourisme.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    
    private final SettingsService settingsService;
    
    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings(
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return ResponseEntity.ok(settingsService.getSettings(lang));
    }
}
