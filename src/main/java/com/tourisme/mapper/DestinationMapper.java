package com.tourisme.mapper;

import com.tourisme.dto.response.DestinationPageCardResponse;
import com.tourisme.dto.response.DestinationResponse;
import com.tourisme.dto.response.DestinationTranslationSnippetResponse;
import com.tourisme.dto.response.PageCardTranslationResponse;
import com.tourisme.entity.Destination;
import com.tourisme.entity.DestinationPageCard;
import com.tourisme.entity.DestinationPageCardTranslation;
import com.tourisme.entity.DestinationTranslation;
import com.tourisme.service.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class DestinationMapper {

    private final TranslationService translationService;

    public DestinationResponse toResponse(Destination destination) {
        return toResponse(destination, "en", false, false);
    }

    public DestinationResponse toResponse(Destination destination, String languageCode) {
        return toResponse(destination, languageCode, false, false);
    }

    public DestinationResponse toResponse(Destination destination, String languageCode, boolean includePageCards) {
        return toResponse(destination, languageCode, includePageCards, false, null);
    }

    /**
     * @param includePageCards load cards (localized when includeCardTranslations is false)
     * @param includeCardTranslations admin shape: EN title/body on card + translations[] per card
     * @param pageCardsOverride when non-null, use this list instead of {@code destination.getPageCards()} (avoids broken FETCH joins)
     */
    public DestinationResponse toResponse(
            Destination destination,
            String languageCode,
            boolean includePageCards,
            boolean includeCardTranslations) {
        return toResponse(destination, languageCode, includePageCards, includeCardTranslations, null);
    }

    public DestinationResponse toResponse(
            Destination destination,
            String languageCode,
            boolean includePageCards,
            boolean includeCardTranslations,
            List<DestinationPageCard> pageCardsOverride) {
        if (destination == null) {
            return null;
        }

        DestinationTranslation translation = translationService.getDestinationTranslation(destination, languageCode);

        List<DestinationPageCardResponse> cards = null;
        if (includePageCards) {
            List<DestinationPageCard> source = pageCardsOverride != null
                    ? pageCardsOverride
                    : (destination.getPageCards() != null ? destination.getPageCards() : List.of());
            if (!source.isEmpty()) {
                cards = source.stream()
                        .sorted(Comparator.comparing(DestinationPageCard::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                        .map(c -> toPageCardResponse(c, languageCode, includeCardTranslations))
                        .collect(toList());
            }
        }

        return DestinationResponse.builder()
                .id(destination.getId())
                .name(translation.getName())
                .slug(destination.getSlug())
                .shortDescription(translation.getShortDescription())
                .fullDescription(translation.getFullDescription())
                .imageUrl(destination.getImageUrl())
                .country(destination.getCountry())
                .city(destination.getCity())
                .featured(destination.getFeatured())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .pageCards(cards)
                .destinationTranslations(null)
                .build();
    }

    /** Admin edit: English destination fields + all destination translations + page cards with translation rows */
    public DestinationResponse toAdminDetailResponse(Destination destination) {
        return toAdminDetailResponse(destination, null);
    }

    public DestinationResponse toAdminDetailResponse(Destination destination, List<DestinationPageCard> pageCardsOverride) {
        if (destination == null) {
            return null;
        }

        List<DestinationTranslationSnippetResponse> destTrans = new ArrayList<>();
        if (destination.getTranslations() != null) {
            for (DestinationTranslation t : destination.getTranslations()) {
                destTrans.add(DestinationTranslationSnippetResponse.builder()
                        .languageCode(t.getLanguageCode())
                        .name(t.getName())
                        .shortDescription(t.getShortDescription())
                        .fullDescription(t.getFullDescription())
                        .build());
            }
        }

        List<DestinationPageCard> cardSource = pageCardsOverride != null
                ? pageCardsOverride
                : (destination.getPageCards() != null ? destination.getPageCards() : List.of());

        List<DestinationPageCardResponse> cards = null;
        if (!cardSource.isEmpty()) {
            cards = cardSource.stream()
                    .sorted(Comparator.comparing(DestinationPageCard::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                    .map(c -> toPageCardResponse(c, "en", true))
                    .collect(toList());
        }

        return DestinationResponse.builder()
                .id(destination.getId())
                .name(destination.getName())
                .slug(destination.getSlug())
                .shortDescription(destination.getShortDescription())
                .fullDescription(destination.getFullDescription())
                .imageUrl(destination.getImageUrl())
                .country(destination.getCountry())
                .city(destination.getCity())
                .featured(destination.getFeatured())
                .createdAt(destination.getCreatedAt())
                .updatedAt(destination.getUpdatedAt())
                .pageCards(cards != null ? cards : List.of())
                .destinationTranslations(destTrans.isEmpty() ? null : destTrans)
                .build();
    }

    private DestinationPageCardResponse toPageCardResponse(
            DestinationPageCard card,
            String languageCode,
            boolean includeCardTranslations) {
        if (includeCardTranslations) {
            List<PageCardTranslationResponse> tr = null;
            if (card.getTranslations() != null && !card.getTranslations().isEmpty()) {
                tr = card.getTranslations().stream()
                        .map(this::toPageCardTranslationResponse)
                        .collect(toList());
            }
            return DestinationPageCardResponse.builder()
                    .id(card.getId())
                    .sortOrder(card.getSortOrder())
                    .imageUrl(card.getImageUrl())
                    .title(card.getTitle())
                    .body(card.getBody())
                    .translations(tr == null || tr.isEmpty() ? null : tr)
                    .build();
        }

        TranslationService.ResolvedPageCard r = translationService.getPageCardTranslation(card, languageCode);
        return DestinationPageCardResponse.builder()
                .id(card.getId())
                .sortOrder(card.getSortOrder())
                .imageUrl(card.getImageUrl())
                .title(r.title())
                .body(r.body())
                .translations(null)
                .build();
    }

    private PageCardTranslationResponse toPageCardTranslationResponse(DestinationPageCardTranslation t) {
        return PageCardTranslationResponse.builder()
                .languageCode(t.getLanguageCode())
                .title(t.getTitle())
                .body(t.getBody())
                .build();
    }
}
