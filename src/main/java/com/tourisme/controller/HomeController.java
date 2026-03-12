package com.tourisme.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tourisme Backend API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("api", "/api");
        response.put("swagger", "/swagger-ui.html");
        response.put("docs", "/api-docs");
        return ResponseEntity.ok(response);
    }
}
