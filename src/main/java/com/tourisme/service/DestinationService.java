package com.tourisme.service;

import com.tourisme.dto.request.DestinationRequest;
import com.tourisme.dto.response.DestinationResponse;
import com.tourisme.entity.Destination;
import com.tourisme.exception.DuplicateResourceException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.DestinationMapper;
import com.tourisme.repository.DestinationRepository;
import com.tourisme.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DestinationService {
    
    private final DestinationRepository destinationRepository;
    private final DestinationMapper destinationMapper;
    
    public Page<DestinationResponse> getAllDestinations(Pageable pageable) {
        return destinationRepository.findAll(pageable)
                .map(destinationMapper::toResponse);
    }
    
    public DestinationResponse getDestinationById(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
        return destinationMapper.toResponse(destination);
    }
    
    public DestinationResponse getDestinationBySlug(String slug) {
        Destination destination = destinationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with slug: " + slug));
        return destinationMapper.toResponse(destination);
    }
    
    @Transactional
    public DestinationResponse createDestination(DestinationRequest request) {
        String slug = SlugUtil.generateSlug(request.getName());
        
        if (destinationRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }
        
        Destination destination = Destination.builder()
                .name(request.getName())
                .slug(slug)
                .shortDescription(request.getShortDescription())
                .fullDescription(request.getFullDescription())
                .imageUrl(request.getImageUrl())
                .country(request.getCountry())
                .city(request.getCity())
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .build();
        
        destination = destinationRepository.save(destination);
        return destinationMapper.toResponse(destination);
    }
    
    @Transactional
    public DestinationResponse updateDestination(Long id, DestinationRequest request) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
        
        if (request.getName() != null && !request.getName().equals(destination.getName())) {
            String slug = SlugUtil.generateSlug(request.getName());
            if (destinationRepository.existsBySlug(slug) && !slug.equals(destination.getSlug())) {
                slug = slug + "-" + System.currentTimeMillis();
            }
            destination.setName(request.getName());
            destination.setSlug(slug);
        }
        
        if (request.getShortDescription() != null) {
            destination.setShortDescription(request.getShortDescription());
        }
        if (request.getFullDescription() != null) {
            destination.setFullDescription(request.getFullDescription());
        }
        if (request.getImageUrl() != null) {
            destination.setImageUrl(request.getImageUrl());
        }
        if (request.getCountry() != null) {
            destination.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            destination.setCity(request.getCity());
        }
        if (request.getFeatured() != null) {
            destination.setFeatured(request.getFeatured());
        }
        
        destination = destinationRepository.save(destination);
        return destinationMapper.toResponse(destination);
    }
    
    @Transactional
    public void deleteDestination(Long id) {
        if (!destinationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Destination not found with id: " + id);
        }
        destinationRepository.deleteById(id);
    }
}
