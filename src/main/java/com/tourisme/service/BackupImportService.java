package com.tourisme.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourisme.dto.response.BackupImportResultResponse;
import com.tourisme.entity.*;
import com.tourisme.exception.BadRequestException;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.ActivityTranslationRepository;
import com.tourisme.repository.BookingRepository;
import com.tourisme.repository.DestinationRepository;
import com.tourisme.repository.DestinationTranslationRepository;
import com.tourisme.repository.FavoriteRepository;
import com.tourisme.repository.ReviewRepository;
import com.tourisme.repository.SettingsRepository;
import com.tourisme.repository.SettingsTranslationRepository;
import com.tourisme.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BackupImportService {

    private static final String FORMAT_VERSION = "1.0";

    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final DestinationRepository destinationRepository;
    private final DestinationTranslationRepository destinationTranslationRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTranslationRepository activityTranslationRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final SettingsRepository settingsRepository;
    private final SettingsTranslationRepository settingsTranslationRepository;

    @Transactional
    public BackupImportResultResponse importFromJson(MultipartFile file, boolean replaceExisting, String defaultPassword) throws Exception {
        if (!replaceExisting) {
            throw new BadRequestException("Only full replace import is supported. Set replaceExisting=true.");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Backup file is required");
        }

        Map<String, Object> root = objectMapper.readValue(file.getBytes(), new TypeReference<>() {});
        String version = str(root.get("formatVersion"));
        if (!FORMAT_VERSION.equals(version)) {
            throw new BadRequestException("Unsupported backup formatVersion: " + version + " (expected " + FORMAT_VERSION + ")");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> userList = castList(root.get("users"));
        if (userList != null && !userList.isEmpty()) {
            if (defaultPassword == null || defaultPassword.length() < 8) {
                throw new BadRequestException("defaultPassword is required (min 8 characters) when the backup contains users.");
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookingsPre = castList(root.get("bookings"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviewsPre = castList(root.get("reviews"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> favoritesPre = castList(root.get("favorites"));
        boolean noUsers = userList == null || userList.isEmpty();
        if (noUsers && bookingsPre != null && !bookingsPre.isEmpty()) {
            throw new BadRequestException("Backup has bookings but no users; cannot import.");
        }
        if (noUsers && reviewsPre != null && !reviewsPre.isEmpty()) {
            throw new BadRequestException("Backup has reviews but no users; cannot import.");
        }
        if (noUsers && favoritesPre != null && !favoritesPre.isEmpty()) {
            throw new BadRequestException("Backup has favorites but no users; cannot import.");
        }

        clearAllApplicationData();

        int dt = 0;
        int at = 0;
        int settingsTransCount = 0;

        Map<Long, Long> oldDestToNew = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> destinations = castList(root.get("destinations"));
        int destCount = 0;
        if (destinations != null) {
            for (Map<String, Object> dm : destinations) {
                Long oldId = toLong(dm.get("id"));
                Destination d = Destination.builder()
                        .name(str(dm.get("name")))
                        .slug(str(dm.get("slug")))
                        .shortDescription(str(dm.get("shortDescription")))
                        .fullDescription(str(dm.get("fullDescription")))
                        .imageUrl(str(dm.get("imageUrl")))
                        .country(str(dm.get("country")))
                        .city(str(dm.get("city")))
                        .featured(toBool(dm.get("featured"), false))
                        .createdAt(parseDateTime(dm.get("createdAt")))
                        .updatedAt(parseDateTime(dm.get("updatedAt")))
                        .build();
                d = destinationRepository.save(d);
                if (oldId != null) {
                    oldDestToNew.put(oldId, d.getId());
                }
                destCount++;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> trans = castList(dm.get("translations"));
                if (trans != null) {
                    for (Map<String, Object> tm : trans) {
                        DestinationTranslation tr = DestinationTranslation.builder()
                                .destination(d)
                                .languageCode(str(tm.get("languageCode")))
                                .name(str(tm.get("name")))
                                .shortDescription(str(tm.get("shortDescription")))
                                .fullDescription(str(tm.get("fullDescription")))
                                .build();
                        destinationTranslationRepository.save(tr);
                        dt++;
                    }
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pageCards = castList(dm.get("pageCards"));
                if (pageCards != null) {
                    for (Map<String, Object> cm : pageCards) {
                        String cardTitle = str(cm.get("title"));
                        if (cardTitle == null || cardTitle.isBlank()) {
                            continue;
                        }
                        DestinationPageCard card = DestinationPageCard.builder()
                                .destination(d)
                                .sortOrder(toInt(cm.get("sortOrder"), 0))
                                .imageUrl(str(cm.get("imageUrl")))
                                .title(cardTitle)
                                .body(str(cm.get("body")))
                                .build();
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> cTrans = castList(cm.get("translations"));
                        if (cTrans != null) {
                            for (Map<String, Object> ctm : cTrans) {
                                String lang = str(ctm.get("languageCode"));
                                if (lang == null || lang.isBlank() || "en".equalsIgnoreCase(lang)) {
                                    continue;
                                }
                                String trTitle = str(ctm.get("title"));
                                DestinationPageCardTranslation ctr = DestinationPageCardTranslation.builder()
                                        .card(card)
                                        .languageCode(lang.toLowerCase())
                                        .title(trTitle != null && !trTitle.isBlank() ? trTitle : card.getTitle())
                                        .body(str(ctm.get("body")))
                                        .build();
                                card.getTranslations().add(ctr);
                            }
                        }
                        d.getPageCards().add(card);
                    }
                    if (!d.getPageCards().isEmpty()) {
                        destinationRepository.save(d);
                    }
                }
            }
        }

        Map<Long, Long> oldActToNew = new HashMap<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activities = castList(root.get("activities"));
        int actCount = 0;
        if (activities != null) {
            for (Map<String, Object> am : activities) {
                Long oldId = toLong(am.get("id"));
                Long oldDestId = toLong(am.get("destinationId"));
                Long newDestId = oldDestId != null ? oldDestToNew.get(oldDestId) : null;
                if (newDestId == null) {
                    throw new BadRequestException("Activity references unknown destinationId: " + oldDestId);
                }
                Destination destRef = destinationRepository.getReferenceById(newDestId);
                BigDecimal price = toBigDecimal(am.get("price"));
                if (price == null) {
                    throw new BadRequestException("Activity " + str(am.get("slug")) + " has no price in backup");
                }

                Activity a = Activity.builder()
                        .title(str(am.get("title")))
                        .slug(str(am.get("slug")))
                        .shortDescription(str(am.get("shortDescription")))
                        .fullDescription(str(am.get("fullDescription")))
                        .price(price)
                        .premiumPrice(toBigDecimal(am.get("premiumPrice")))
                        .budgetPrice(toBigDecimal(am.get("budgetPrice")))
                        .duration(str(am.get("duration")))
                        .location(str(am.get("location")))
                        .category(str(am.get("category")))
                        .difficultyLevel(parseEnum(am.get("difficultyLevel"), Activity.DifficultyLevel.class))
                        .tourType(parseEnum(am.get("tourType"), Activity.TourType.class, Activity.TourType.SHARED))
                        .ratingAverage(toBigDecimal(am.get("ratingAverage"), BigDecimal.ZERO))
                        .reviewCount(toInt(am.get("reviewCount"), 0))
                        .featured(toBool(am.get("featured"), false))
                        .active(toBool(am.get("active"), true))
                        .maxGroupSize(toInt(am.get("maxGroupSize"), 20))
                        .availableSlots(toInt(am.get("availableSlots"), 50))
                        .imageUrl(str(am.get("imageUrl")))
                        .galleryImages(new ArrayList<>(stringList(am.get("galleryImages"))))
                        .includedItems(new ArrayList<>(stringList(am.get("includedItems"))))
                        .excludedItems(new ArrayList<>(stringList(am.get("excludedItems"))))
                        .itinerary(new ArrayList<>(stringList(am.get("itinerary"))))
                        .availableDates(new ArrayList<>(dateList(am.get("availableDates"))))
                        .departureLocation(str(am.get("departureLocation")))
                        .returnLocation(str(am.get("returnLocation")))
                        .meetingTime(str(am.get("meetingTime")))
                        .availability(str(am.get("availability")))
                        .whatToExpect(str(am.get("whatToExpect")))
                        .complementaries(new ArrayList<>(stringList(am.get("complementaries"))))
                        .mapUrl(str(am.get("mapUrl")))
                        .destination(destRef)
                        .createdAt(parseDateTime(am.get("createdAt")))
                        .updatedAt(parseDateTime(am.get("updatedAt")))
                        .build();
                a = activityRepository.save(a);
                if (oldId != null) {
                    oldActToNew.put(oldId, a.getId());
                }
                actCount++;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> aTrans = castList(am.get("translations"));
                if (aTrans != null) {
                    for (Map<String, Object> tm : aTrans) {
                        ActivityTranslation tr = ActivityTranslation.builder()
                                .activity(a)
                                .languageCode(str(tm.get("languageCode")))
                                .title(str(tm.get("title")))
                                .shortDescription(str(tm.get("shortDescription")))
                                .fullDescription(str(tm.get("fullDescription")))
                                .location(str(tm.get("location")))
                                .category(str(tm.get("category")))
                                .departureLocation(str(tm.get("departureLocation")))
                                .returnLocation(str(tm.get("returnLocation")))
                                .meetingTime(str(tm.get("meetingTime")))
                                .availability(str(tm.get("availability")))
                                .whatToExpect(str(tm.get("whatToExpect")))
                                .build();
                        activityTranslationRepository.save(tr);
                        at++;
                    }
                }
            }
        }

        Map<Long, Long> oldUserToNew = new HashMap<>();
        int userCount = 0;
        if (userList != null) {
            String encoded = passwordEncoder.encode(defaultPassword);
            for (Map<String, Object> um : userList) {
                Long oldId = toLong(um.get("id"));
                User.Role role;
                try {
                    role = User.Role.valueOf(str(um.get("role")));
                } catch (Exception e) {
                    role = User.Role.ROLE_CLIENT;
                }
                User u = User.builder()
                        .firstName(str(um.get("firstName")))
                        .lastName(str(um.get("lastName")))
                        .email(str(um.get("email")))
                        .password(encoded)
                        .phone(str(um.get("phone")))
                        .role(role)
                        .active(toBool(um.get("active"), true))
                        .createdAt(parseDateTime(um.get("createdAt")))
                        .updatedAt(parseDateTime(um.get("updatedAt")))
                        .build();
                u = userRepository.save(u);
                if (oldId != null) {
                    oldUserToNew.put(oldId, u.getId());
                }
                userCount++;
            }
        }

        int bookingCount = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bookings = castList(root.get("bookings"));
        if (bookings != null) {
            for (Map<String, Object> bm : bookings) {
                Long uid = oldUserToNew.get(toLong(bm.get("userId")));
                Long aid = oldActToNew.get(toLong(bm.get("activityId")));
                if (uid == null || aid == null) {
                    throw new BadRequestException("Booking references missing user or activity mapping");
                }
                BigDecimal total = toBigDecimal(bm.get("totalPrice"));
                if (total == null) {
                    total = BigDecimal.ZERO;
                }
                Booking b = Booking.builder()
                        .bookingReference(str(bm.get("bookingReference")))
                        .user(userRepository.getReferenceById(uid))
                        .activity(activityRepository.getReferenceById(aid))
                        .bookingDate(parseDate(bm.get("bookingDate")))
                        .travelDate(parseDate(bm.get("travelDate")))
                        .numberOfPeople(toInt(bm.get("numberOfPeople"), 1))
                        .totalPrice(total)
                        .status(parseEnum(bm.get("status"), Booking.BookingStatus.class, Booking.BookingStatus.PENDING))
                        .specialRequest(str(bm.get("specialRequest")))
                        .createdAt(parseDateTime(bm.get("createdAt")))
                        .updatedAt(parseDateTime(bm.get("updatedAt")))
                        .build();
                bookingRepository.save(b);
                bookingCount++;
            }
        }

        int reviewCount = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> reviews = castList(root.get("reviews"));
        if (reviews != null) {
            for (Map<String, Object> rm : reviews) {
                Long uid = oldUserToNew.get(toLong(rm.get("userId")));
                Long aid = oldActToNew.get(toLong(rm.get("activityId")));
                if (uid == null || aid == null) {
                    throw new BadRequestException("Review references missing user or activity mapping");
                }
                Review r = Review.builder()
                        .user(userRepository.getReferenceById(uid))
                        .activity(activityRepository.getReferenceById(aid))
                        .rating(toInt(rm.get("rating"), 0))
                        .comment(str(rm.get("comment")))
                        .approved(toBool(rm.get("approved"), false))
                        .createdAt(parseDateTime(rm.get("createdAt")))
                        .updatedAt(parseDateTime(rm.get("updatedAt")))
                        .build();
                reviewRepository.save(r);
                reviewCount++;
            }
        }

        int favCount = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> favorites = castList(root.get("favorites"));
        if (favorites != null) {
            for (Map<String, Object> fm : favorites) {
                Long uid = oldUserToNew.get(toLong(fm.get("userId")));
                Long aid = oldActToNew.get(toLong(fm.get("activityId")));
                if (uid == null || aid == null) {
                    throw new BadRequestException("Favorite references missing user or activity mapping");
                }
                Favorite f = Favorite.builder()
                        .user(userRepository.getReferenceById(uid))
                        .activity(activityRepository.getReferenceById(aid))
                        .createdAt(parseDateTime(fm.get("createdAt")))
                        .build();
                favoriteRepository.save(f);
                favCount++;
            }
        }

        int settingsRows = 0;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> settingsList = castList(root.get("settings"));
        if (settingsList != null) {
            for (Map<String, Object> sm : settingsList) {
                Settings s = Settings.builder()
                        .siteName(str(sm.get("siteName")))
                        .logoUrl(str(sm.get("logoUrl")))
                        .contactEmail(str(sm.get("contactEmail")))
                        .contactPhone(str(sm.get("contactPhone")))
                        .address(str(sm.get("address")))
                        .facebookUrl(str(sm.get("facebookUrl")))
                        .instagramUrl(str(sm.get("instagramUrl")))
                        .twitterUrl(str(sm.get("twitterUrl")))
                        .youtubeUrl(str(sm.get("youtubeUrl")))
                        .bannerTitle(str(sm.get("bannerTitle")))
                        .bannerSubtitle(str(sm.get("bannerSubtitle")))
                        .updatedAt(parseDateTime(sm.get("updatedAt")))
                        .build();
                s = settingsRepository.save(s);
                settingsRows++;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> strans = castList(sm.get("translations"));
                if (strans != null) {
                    for (Map<String, Object> tm : strans) {
                        SettingsTranslation tr = SettingsTranslation.builder()
                                .settings(s)
                                .languageCode(str(tm.get("languageCode")))
                                .siteName(str(tm.get("siteName")))
                                .bannerTitle(str(tm.get("bannerTitle")))
                                .bannerSubtitle(str(tm.get("bannerSubtitle")))
                                .address(str(tm.get("address")))
                                .build();
                        settingsTranslationRepository.save(tr);
                        settingsTransCount++;
                    }
                }
            }
        }

        entityManager.flush();

        return BackupImportResultResponse.builder()
                .destinations(destCount)
                .destinationTranslations(dt)
                .activities(actCount)
                .activityTranslations(at)
                .users(userCount)
                .bookings(bookingCount)
                .reviews(reviewCount)
                .favorites(favCount)
                .settingsRows(settingsRows)
                .settingsTranslations(settingsTransCount)
                .message("Import completed. Sign in again: existing sessions are invalid. All imported users share the password you provided.")
                .build();
    }

    private void clearAllApplicationData() {
        entityManager.createNativeQuery("DELETE FROM favorites").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM reviews").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM bookings").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_translations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_gallery_images").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_included_items").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_excluded_items").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_itinerary").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_available_dates").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activity_complementaries").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM activities").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM destination_translations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM destinations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM settings_translations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM settings").executeUpdate();
        entityManager.flush();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object o) {
        if (o == null) {
            return null;
        }
        if (!(o instanceof List<?> list)) {
            return null;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                out.add((Map<String, Object>) m);
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(o.toString());
    }

    private static int toInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(o.toString());
    }

    private static boolean toBool(Object o, boolean def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(o.toString());
    }

    private static BigDecimal toBigDecimal(Object o) {
        return toBigDecimal(o, null);
    }

    private static BigDecimal toBigDecimal(Object o, BigDecimal def) {
        if (o == null) {
            return def;
        }
        if (o instanceof BigDecimal bd) {
            return bd;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return new BigDecimal(o.toString());
    }

    private static LocalDate parseDate(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        return LocalDate.parse(o.toString());
    }

    private static LocalDateTime parseDateTime(Object o) {
        if (o == null) {
            return LocalDateTime.now();
        }
        if (o instanceof LocalDateTime dt) {
            return dt;
        }
        if (o instanceof List<?> list && list.size() >= 3) {
            int y = toInt(list.get(0), 1970);
            int m = toInt(list.get(1), 1);
            int d = toInt(list.get(2), 1);
            int h = list.size() > 3 ? toInt(list.get(3), 0) : 0;
            int min = list.size() > 4 ? toInt(list.get(4), 0) : 0;
            int sec = list.size() > 5 ? toInt(list.get(5), 0) : 0;
            return LocalDateTime.of(y, m, d, h, min, sec);
        }
        String s = o.toString().trim();
        if (s.length() == 10) {
            return LocalDate.parse(s).atStartOfDay();
        }
        try {
            if (s.contains("T")) {
                String core = s.length() >= 19 ? s.substring(0, 19) : s;
                return LocalDateTime.parse(core);
            }
            return LocalDateTime.parse(s.replace(" ", "T"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private static List<String> stringList(Object o) {
        if (o == null || !(o instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<String> out = new ArrayList<>();
        for (Object x : list) {
            if (x != null) {
                out.add(x.toString());
            }
        }
        return out;
    }

    private static List<LocalDate> dateList(Object o) {
        if (o == null || !(o instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<LocalDate> out = new ArrayList<>();
        for (Object x : list) {
            if (x != null) {
                out.add(parseDate(x));
            }
        }
        return out;
    }

    private static <E extends Enum<E>> E parseEnum(Object o, Class<E> type) {
        if (o == null) {
            return null;
        }
        return Enum.valueOf(type, o.toString());
    }

    private static <E extends Enum<E>> E parseEnum(Object o, Class<E> type, E def) {
        if (o == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, o.toString());
        } catch (Exception e) {
            return def;
        }
    }
}
