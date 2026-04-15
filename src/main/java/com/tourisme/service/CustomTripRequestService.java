package com.tourisme.service;

import com.tourisme.dto.request.CustomTripRequestCreateRequest;
import com.tourisme.dto.response.CustomTripRequestResponse;
import com.tourisme.entity.CustomTripRequest;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.repository.CustomTripRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class CustomTripRequestService {

    private final CustomTripRequestRepository customTripRequestRepository;
    private final UserNotificationService userNotificationService;

    @Transactional
    public CustomTripRequestResponse create(CustomTripRequestCreateRequest request) {
        CustomTripRequest saved = customTripRequestRepository.save(
                CustomTripRequest.builder()
                        .name(request.getName().trim())
                        .email(request.getEmail().trim())
                        .phone(request.getPhone() == null ? null : request.getPhone().trim())
                        .startCity(request.getStartCity().trim())
                        .destinationCity(request.getDestinationCity().trim())
                        .preferredDate(request.getPreferredDate())
                        .numberOfPeople(request.getNumberOfPeople())
                        .message(request.getMessage() == null ? null : request.getMessage().trim())
                        .status(CustomTripRequest.Status.PENDING)
                        .build()
        );

        userNotificationService.notifyAdminsCustomTripRequest(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CustomTripRequestResponse> findAll(Pageable pageable) {
        return customTripRequestRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public CustomTripRequestResponse updateStatus(Long id, CustomTripRequest.Status status) {
        CustomTripRequest r = customTripRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Custom trip request not found with id: " + id));
        r.setStatus(status);
        return toResponse(customTripRequestRepository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        if (!customTripRequestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Custom trip request not found with id: " + id);
        }
        customTripRequestRepository.deleteById(id);
    }

    private CustomTripRequestResponse toResponse(CustomTripRequest e) {
        return CustomTripRequestResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .email(e.getEmail())
                .phone(e.getPhone())
                .startCity(e.getStartCity())
                .destinationCity(e.getDestinationCity())
                .preferredDate(e.getPreferredDate())
                .numberOfPeople(e.getNumberOfPeople())
                .message(e.getMessage())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}

