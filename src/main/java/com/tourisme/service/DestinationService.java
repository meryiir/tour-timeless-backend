package com.tourisme.service;

import com.tourisme.dto.request.DestinationPageCardRequest;
import com.tourisme.dto.request.DestinationPageCardTranslationRequest;
import com.tourisme.dto.request.DestinationRequest;
import com.tourisme.dto.request.DestinationTranslationRequest;
import com.tourisme.dto.response.DestinationResponse;
import com.tourisme.entity.Destination;
import com.tourisme.entity.DestinationPageCard;
import com.tourisme.entity.DestinationPageCardTranslation;
import com.tourisme.entity.DestinationTranslation;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.DestinationMapper;
import com.tourisme.repository.DestinationPageCardRepository;
import com.tourisme.repository.DestinationRepository;
import com.tourisme.repository.DestinationTranslationRepository;
import com.tourisme.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService {

    private final DestinationRepository destinationRepository;
    private final DestinationTranslationRepository destinationTranslationRepository;
    private final DestinationPageCardRepository destinationPageCardRepository;
    private final DestinationMapper destinationMapper;

    public Page<DestinationResponse> getAllDestinations(Pageable pageable) {
        return getAllDestinations(pageable, "en");
    }

    public Page<DestinationResponse> getAllDestinations(Pageable pageable, String languageCode) {
        // Featured first, then name — avoids “newest id last” so home page (first page) shows catalog destinations reliably.
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.asc("name")));
        return destinationRepository.findAll(sorted)
                .map(dest -> destinationMapper.toResponse(dest, languageCode, false, false));
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationById(Long id) {
        return getDestinationById(id, "en");
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationById(Long id, String languageCode) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
        List<DestinationPageCard> pageCards = destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(id);
        return destinationMapper.toResponse(destination, languageCode, true, false, pageCards);
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationBySlug(String slug) {
        return getDestinationBySlug(slug, "en");
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationBySlug(String slug, String languageCode) {
        Destination destination = destinationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with slug: " + slug));
        List<DestinationPageCard> pageCards =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(destination.getId());
        return destinationMapper.toResponse(destination, languageCode, true, false, pageCards);
    }

    @Transactional(readOnly = true)
    public DestinationResponse getDestinationForAdmin(Long id) {
        Destination destination = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
        if (destination.getTranslations() != null) {
            destination.getTranslations().size();
        }
        List<DestinationPageCard> pageCards = destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(id);
        return destinationMapper.toAdminDetailResponse(destination, pageCards);
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

        if (request.getTranslations() != null && !request.getTranslations().isEmpty()) {
            for (DestinationTranslationRequest translationRequest : request.getTranslations()) {
                DestinationTranslation translation = DestinationTranslation.builder()
                        .destination(destination)
                        .languageCode(translationRequest.getLanguageCode())
                        .name(translationRequest.getName())
                        .shortDescription(translationRequest.getShortDescription())
                        .fullDescription(translationRequest.getFullDescription())
                        .build();
                destinationTranslationRepository.save(translation);
            }
        }

        syncPageCards(destination, request.getPageCards());
        destinationRepository.save(destination);

        Destination reloaded = destinationRepository.findById(destination.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found after create"));
        if (reloaded.getTranslations() != null) {
            reloaded.getTranslations().size();
        }
        List<DestinationPageCard> reloadedCards =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(reloaded.getId());
        return destinationMapper.toAdminDetailResponse(reloaded, reloadedCards);
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

        if (request.getTranslations() != null && !request.getTranslations().isEmpty()) {
            destinationTranslationRepository.deleteAll(destination.getTranslations());

            for (DestinationTranslationRequest translationRequest : request.getTranslations()) {
                DestinationTranslation translation = DestinationTranslation.builder()
                        .destination(destination)
                        .languageCode(translationRequest.getLanguageCode())
                        .name(translationRequest.getName())
                        .shortDescription(translationRequest.getShortDescription())
                        .fullDescription(translationRequest.getFullDescription())
                        .build();
                destinationTranslationRepository.save(translation);
            }
        }

        if (request.getPageCards() != null) {
            syncPageCards(destination, request.getPageCards());
            destinationRepository.save(destination);
        }

        Destination reloaded = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
        if (reloaded.getTranslations() != null) {
            reloaded.getTranslations().size();
        }
        List<DestinationPageCard> reloadedCards = destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(id);
        return destinationMapper.toAdminDetailResponse(reloaded, reloadedCards);
    }

    private void syncPageCards(Destination destination, List<DestinationPageCardRequest> requests) {
        if (requests == null) {
            return;
        }
        destination.getPageCards().clear();
        int order = 0;
        for (DestinationPageCardRequest req : requests) {
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                continue;
            }
            DestinationPageCard card = DestinationPageCard.builder()
                    .destination(destination)
                    .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : order)
                    .imageUrl(req.getImageUrl())
                    .title(req.getTitle().trim())
                    .body(req.getBody())
                    .build();
            if (req.getTranslations() != null) {
                for (DestinationPageCardTranslationRequest tr : req.getTranslations()) {
                    if (tr.getLanguageCode() == null || "en".equalsIgnoreCase(tr.getLanguageCode())) {
                        continue;
                    }
                    String tTitle = tr.getTitle() != null && !tr.getTitle().isBlank() ? tr.getTitle().trim() : card.getTitle();
                    DestinationPageCardTranslation t = DestinationPageCardTranslation.builder()
                            .card(card)
                            .languageCode(tr.getLanguageCode().toLowerCase())
                            .title(tTitle)
                            .body(tr.getBody() != null ? tr.getBody() : "")
                            .build();
                    card.getTranslations().add(t);
                }
            }
            destination.getPageCards().add(card);
            order++;
        }
    }

    @Transactional
    public void deleteDestination(Long id) {
        if (!destinationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Destination not found with id: " + id);
        }
        destinationRepository.deleteById(id);
    }
}
