package com.tourisme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourisme.entity.*;
import com.tourisme.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final ObjectMapper objectMapper;
    private final DestinationRepository destinationRepository;
    private final DestinationTranslationRepository destinationTranslationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTranslationRepository activityTranslationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final SettingsRepository settingsRepository;

    @Transactional(readOnly = true)
    public byte[] exportJsonPretty() throws Exception {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("formatVersion", "1.0");
        root.put("exportedAt", Instant.now().toString());
        root.put("application", "tourisme-backend");
        root.put("note", "User password hashes are not included for security. This file is for backup / migration reference.");

        List<DestinationTranslation> destTranslations = destinationTranslationRepository.findAll();
        Map<Long, List<DestinationTranslation>> destTransByDestId = destTranslations.stream()
                .collect(Collectors.groupingBy(t -> t.getDestination().getId()));

        List<Map<String, Object>> destinations = new ArrayList<>();
        for (Destination d : destinationRepository.findAll()) {
            Hibernate.initialize(d.getPageCards());
            List<Map<String, Object>> transMaps = destTransByDestId.getOrDefault(d.getId(), List.of()).stream()
                    .map(this::toDestinationTranslationMap)
                    .toList();
            destinations.add(toDestinationMap(d, transMaps));
        }
        root.put("destinations", destinations);

        List<ActivityTranslation> actTranslations = activityTranslationRepository.findAll();
        Map<Long, List<ActivityTranslation>> actTransByActivityId = actTranslations.stream()
                .collect(Collectors.groupingBy(t -> t.getActivity().getId()));

        List<Map<String, Object>> activities = new ArrayList<>();
        for (Activity a : activityRepository.findAll()) {
            Hibernate.initialize(a.getGalleryImages());
            Hibernate.initialize(a.getIncludedItems());
            Hibernate.initialize(a.getExcludedItems());
            Hibernate.initialize(a.getItinerary());
            Hibernate.initialize(a.getAvailableDates());
            Hibernate.initialize(a.getComplementaries());
            Hibernate.initialize(a.getDestination());
            List<Map<String, Object>> aTrans = actTransByActivityId.getOrDefault(a.getId(), List.of()).stream()
                    .map(this::toActivityTranslationMap)
                    .toList();
            activities.add(toActivityMap(a, aTrans));
        }
        root.put("activities", activities);

        root.put("users", userRepository.findAll().stream().map(this::toUserMap).toList());

        List<Map<String, Object>> bookings = new ArrayList<>();
        for (Booking b : bookingRepository.findAll()) {
            Hibernate.initialize(b.getUser());
            Hibernate.initialize(b.getActivity());
            bookings.add(toBookingMap(b));
        }
        root.put("bookings", bookings);

        List<Map<String, Object>> reviews = new ArrayList<>();
        for (Review r : reviewRepository.findAll()) {
            Hibernate.initialize(r.getUser());
            Hibernate.initialize(r.getActivity());
            reviews.add(toReviewMap(r));
        }
        root.put("reviews", reviews);

        List<Map<String, Object>> favorites = new ArrayList<>();
        for (Favorite f : favoriteRepository.findAll()) {
            Hibernate.initialize(f.getUser());
            Hibernate.initialize(f.getActivity());
            favorites.add(toFavoriteMap(f));
        }
        root.put("favorites", favorites);

        List<Map<String, Object>> settingsList = new ArrayList<>();
        for (Settings s : settingsRepository.findAll()) {
            Hibernate.initialize(s.getTranslations());
            settingsList.add(toSettingsMap(s));
        }
        root.put("settings", settingsList);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
    }

    private Map<String, Object> toDestinationMap(Destination d, List<Map<String, Object>> translations) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("slug", d.getSlug());
        m.put("shortDescription", d.getShortDescription());
        m.put("fullDescription", d.getFullDescription());
        m.put("imageUrl", d.getImageUrl());
        m.put("country", d.getCountry());
        m.put("city", d.getCity());
        m.put("featured", d.getFeatured());
        m.put("createdAt", d.getCreatedAt());
        m.put("updatedAt", d.getUpdatedAt());
        m.put("translations", translations);
        List<Map<String, Object>> pageCards = new ArrayList<>();
        if (d.getPageCards() != null) {
            for (com.tourisme.entity.DestinationPageCard card : d.getPageCards()) {
                Hibernate.initialize(card.getTranslations());
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("sortOrder", card.getSortOrder());
                cm.put("imageUrl", card.getImageUrl());
                cm.put("title", card.getTitle());
                cm.put("body", card.getBody());
                List<Map<String, Object>> cTrans = card.getTranslations().stream()
                        .map(this::toDestinationPageCardTranslationMap)
                        .toList();
                cm.put("translations", cTrans);
                pageCards.add(cm);
            }
        }
        m.put("pageCards", pageCards);
        return m;
    }

    private Map<String, Object> toDestinationPageCardTranslationMap(com.tourisme.entity.DestinationPageCardTranslation t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("languageCode", t.getLanguageCode());
        m.put("title", t.getTitle());
        m.put("body", t.getBody());
        return m;
    }

    private Map<String, Object> toDestinationTranslationMap(DestinationTranslation t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("destinationId", t.getDestination().getId());
        m.put("languageCode", t.getLanguageCode());
        m.put("name", t.getName());
        m.put("shortDescription", t.getShortDescription());
        m.put("fullDescription", t.getFullDescription());
        return m;
    }

    private Map<String, Object> toActivityMap(Activity a, List<Map<String, Object>> translations) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("destinationId", a.getDestination().getId());
        m.put("title", a.getTitle());
        m.put("slug", a.getSlug());
        m.put("shortDescription", a.getShortDescription());
        m.put("fullDescription", a.getFullDescription());
        m.put("price", a.getPrice());
        m.put("premiumPrice", a.getPremiumPrice());
        m.put("budgetPrice", a.getBudgetPrice());
        m.put("duration", a.getDuration());
        m.put("location", a.getLocation());
        m.put("category", a.getCategory());
        m.put("difficultyLevel", a.getDifficultyLevel() != null ? a.getDifficultyLevel().name() : null);
        m.put("tourType", a.getTourType() != null ? a.getTourType().name() : null);
        m.put("ratingAverage", a.getRatingAverage());
        m.put("reviewCount", a.getReviewCount());
        m.put("featured", a.getFeatured());
        m.put("active", a.getActive());
        m.put("maxGroupSize", a.getMaxGroupSize());
        m.put("availableSlots", a.getAvailableSlots());
        m.put("imageUrl", a.getImageUrl());
        m.put("galleryImages", new ArrayList<>(a.getGalleryImages()));
        m.put("includedItems", new ArrayList<>(a.getIncludedItems()));
        m.put("excludedItems", new ArrayList<>(a.getExcludedItems()));
        m.put("itinerary", new ArrayList<>(a.getItinerary()));
        m.put("availableDates", new ArrayList<>(a.getAvailableDates()));
        m.put("departureLocation", a.getDepartureLocation());
        m.put("returnLocation", a.getReturnLocation());
        m.put("meetingTime", a.getMeetingTime());
        m.put("availability", a.getAvailability());
        m.put("whatToExpect", a.getWhatToExpect());
        m.put("complementaries", new ArrayList<>(a.getComplementaries()));
        m.put("mapUrl", a.getMapUrl());
        m.put("createdAt", a.getCreatedAt());
        m.put("updatedAt", a.getUpdatedAt());
        m.put("translations", translations);
        return m;
    }

    private Map<String, Object> toActivityTranslationMap(ActivityTranslation t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("activityId", t.getActivity().getId());
        m.put("languageCode", t.getLanguageCode());
        m.put("title", t.getTitle());
        m.put("shortDescription", t.getShortDescription());
        m.put("fullDescription", t.getFullDescription());
        m.put("location", t.getLocation());
        m.put("category", t.getCategory());
        m.put("departureLocation", t.getDepartureLocation());
        m.put("returnLocation", t.getReturnLocation());
        m.put("meetingTime", t.getMeetingTime());
        m.put("availability", t.getAvailability());
        m.put("whatToExpect", t.getWhatToExpect());
        return m;
    }

    private Map<String, Object> toUserMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole().name());
        m.put("active", u.getActive());
        m.put("createdAt", u.getCreatedAt());
        m.put("updatedAt", u.getUpdatedAt());
        m.put("password", "[REDACTED]");
        return m;
    }

    private Map<String, Object> toBookingMap(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("bookingReference", b.getBookingReference());
        m.put("userId", b.getUser().getId());
        m.put("activityId", b.getActivity().getId());
        m.put("bookingDate", b.getBookingDate());
        m.put("travelDate", b.getTravelDate());
        m.put("numberOfPeople", b.getNumberOfPeople());
        m.put("totalPrice", b.getTotalPrice());
        m.put("status", b.getStatus().name());
        m.put("specialRequest", b.getSpecialRequest());
        m.put("createdAt", b.getCreatedAt());
        m.put("updatedAt", b.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toReviewMap(Review r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("userId", r.getUser().getId());
        m.put("activityId", r.getActivity().getId());
        m.put("rating", r.getRating());
        m.put("comment", r.getComment());
        m.put("approved", r.getApproved());
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toFavoriteMap(Favorite f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId());
        m.put("userId", f.getUser().getId());
        m.put("activityId", f.getActivity().getId());
        m.put("createdAt", f.getCreatedAt());
        return m;
    }

    private Map<String, Object> toSettingsMap(Settings s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("siteName", s.getSiteName());
        m.put("logoUrl", s.getLogoUrl());
        m.put("contactEmail", s.getContactEmail());
        m.put("contactPhone", s.getContactPhone());
        m.put("address", s.getAddress());
        m.put("facebookUrl", s.getFacebookUrl());
        m.put("instagramUrl", s.getInstagramUrl());
        m.put("twitterUrl", s.getTwitterUrl());
        m.put("youtubeUrl", s.getYoutubeUrl());
        m.put("bannerTitle", s.getBannerTitle());
        m.put("bannerSubtitle", s.getBannerSubtitle());
        m.put("updatedAt", s.getUpdatedAt());
        List<Map<String, Object>> trans = s.getTranslations().stream().map(this::toSettingsTranslationMap).toList();
        m.put("translations", trans);
        return m;
    }

    private Map<String, Object> toSettingsTranslationMap(SettingsTranslation t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("settingsId", t.getSettings().getId());
        m.put("languageCode", t.getLanguageCode());
        m.put("siteName", t.getSiteName());
        m.put("bannerTitle", t.getBannerTitle());
        m.put("bannerSubtitle", t.getBannerSubtitle());
        m.put("address", t.getAddress());
        return m;
    }
}
