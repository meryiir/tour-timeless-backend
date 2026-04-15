package com.tourisme.controller;

import com.tourisme.dto.request.CustomTripRequestCreateRequest;
import com.tourisme.dto.response.CustomTripRequestResponse;
import com.tourisme.service.CustomTripRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/custom-trip-requests")
@RequiredArgsConstructor
public class CustomTripRequestController {

    private final CustomTripRequestService customTripRequestService;

    /** Public endpoint: visitors can request a custom trip. */
    @PostMapping
    public ResponseEntity<CustomTripRequestResponse> create(@Valid @RequestBody CustomTripRequestCreateRequest request) {
        return new ResponseEntity<>(customTripRequestService.create(request), HttpStatus.CREATED);
    }
}

