package com.tourisme.config;

import com.tourisme.entity.*;
import com.tourisme.repository.*;
import com.tourisme.util.BookingReferenceUtil;
import com.tourisme.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final DestinationPageCardRepository destinationPageCardRepository;
    private final ActivityRepository activityRepository;
    private final ActivityTranslationRepository activityTranslationRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * When false, {@link CommandLineRunner#run} returns immediately — no seed logic runs (PostgreSQL is untouched by this component).
     */
    @Value("${app.seeding.run-on-startup:true}")
    private boolean runSeedingOnStartup;

    /**
     * When false (default), Tour-in-Morocco reference copy is not re-applied on every startup, so DB content persists across restarts.
     */
    @Value("${app.seeding.apply-tour-in-morocco-reference:false}")
    private boolean applyTourInMoroccoReference;

    /** When false (default), Sahara keyword–matched activities are not re-assigned to the Sahara destination on each start. */
    @Value("${app.seeding.relink-sahara-activities:false}")
    private boolean relinkSaharaActivities;

    /** When false (default), inactive activities stay inactive after restart (admin “deactivate” is respected). */
    @Value("${app.seeding.reactivate-all-activities:false}")
    private boolean reactivateAllActivities;

    /**
     * When false (default), destinations removed in admin are not recreated on startup (fixes “delete comes back” after restart).
     * When true, missing Marrakech/Sahara/Ouzoud and default Marrakech page cards are re-inserted like older seeders.
     */
    @Value("${app.seeding.recreate-missing-default-destinations:false}")
    private boolean recreateMissingDefaultDestinations;

    /**
     * When true, {@link #seedOuzoudWaterfallsIfNeeded()}, {@link #seedOurikaValleyIfNeeded()}, and
     * {@link #seedMarrakechPalmGroveIfNeeded()} run for non-empty DBs if those slugs are missing.
     * Default true (see {@code application.yml}): missing catalog destinations are inserted on startup.
     * Set {@code app.seeding.seed-catalog-day-trip-destinations=false} if you removed a catalog destination and must not re-create it.
     */
    @Value("${app.seeding.seed-catalog-day-trip-destinations:true}")
    private boolean seedCatalogDayTripDestinations;

    /**
     * Copy aligned with https://www.tour-in-morocco.com/tour-destination/ouzoud-waterfalls/
     */
    private static final String OUZOUD_TOUR_IN_MOROCCO_INTRO =
            "The people of this country are super friendly and welcoming. Tour in Morocco invites you to come and explore the pristine beauty of this country, Sahara, Mountains, and Beaches. For your convenience, we have designed and tailor-made amazing Moroccan trip packages and Sahara desert Tours. Let us take care of all the worries of your trips and tours to Morocco.";

    /** Main listing paragraph from the reference “Marrakech Day Trip to Ouzoud Waterfalls” card (ellipsis on site completed for our app). */
    private static final String OUZOUD_REFERENCE_LISTING_PARAGRAPH =
            "150 kilometers from Marrakech, the Ouzoud waterfalls constitute a breathtaking landscape. It is not for nothing that the waterfalls are classified as one of the most beautiful sites in Morocco! Nestled in the heart of lush greenery, there are 3 waterfalls, the highest of which reaches 110 meters high! After 1h30 by car, we arrive at Ouzoud.";

    private static final String OUZOUD_DESTINATION_SHORT = OUZOUD_REFERENCE_LISTING_PARAGRAPH;

    private static final String OUZOUD_DESTINATION_FULL =
            OUZOUD_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + OUZOUD_REFERENCE_LISTING_PARAGRAPH
                    + " Walk the footpaths along the El Abid River gorge, take in the viewpoints above the cascades, watch Barbary macaques in the olive groves, and enjoy optional boat rides and lunch with a view before returning to Marrakech.";

    private static final String OUZOUD_ACTIVITY_SHORT =
            OUZOUD_REFERENCE_LISTING_PARAGRAPH
                    + " Spend the day exploring the falls, the village, and the surrounding nature on this Marrakech day trip.";

    private static final String OUZOUD_ACTIVITY_FULL =
            OUZOUD_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + OUZOUD_REFERENCE_LISTING_PARAGRAPH
                    + "\n\n"
                    + "Morning pick-up in Marrakech and scenic drive through the Middle Atlas foothills. At Ouzoud, follow marked trails to upper and lower viewpoints, feel the spray from the main cascades, and photograph the rainbows on sunny days. Free time for lunch at a terrace restaurant, optional boat trips at the base of the falls, and relaxed wandering before the return transfer to Marrakech.";

    /**
     * Copy aligned with https://www.tour-in-morocco.com/tour-destination/ourika-valley/
     */
    private static final String OURIKA_TOUR_IN_MOROCCO_INTRO = OUZOUD_TOUR_IN_MOROCCO_INTRO;

    private static final String OURIKA_DESTINATION_SHORT =
            "Ourika Valley, Atlas Mountains — Marrakech day trips from riverside walks and Berber villages to mint tea in the foothills.";

    private static final String OURIKA_DESTINATION_FULL =
            OURIKA_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + "Ourika Valley lies in the High Atlas foothills near Marrakech — a green corridor of terraced fields, argan trees, and Berber villages along the Ourika River. Choose a relaxed day by the water, a stroll through Setti Fatma, or combine with wider Atlas and “three valleys” routes.\n\n"
                    + "The Atlas Mountains are in fact three distinct ranges that run in bands across Morocco’s interior, dividing it into strips of lower-lying land. Furthest north is the Middle Atlas, while the southerly range is the Anti-Atlas that helps hold back the Western Sahara — with the High Atlas rising between, framing valleys like Ourika for unforgettable day trips.";

    private static final String OURIKA_3_VALLEYS_LISTING =
            "The Atlas Mountains are in fact three distinct ranges that run in bands across Morocco’s interior, dividing it into strips of lower-lying land. Furthest north in the Middle Atlas, while the southerly range is the Anti Atlas that attempt to keep desolate Western Sahara at bay.";

    private static final String OURIKA_3_VALLEYS_ACTIVITY_SHORT = OURIKA_3_VALLEYS_LISTING;

    private static final String OURIKA_3_VALLEYS_ACTIVITY_FULL =
            OURIKA_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + OURIKA_3_VALLEYS_LISTING
                    + "\n\n"
                    + "This adventure-style day trip crosses dramatic High Atlas scenery and explores multiple valley characters — from forested slopes to rocky gorges — with photo stops and time to feel the scale of the range on a route designed for wide landscapes and big skies.";

    private static final String OURIKA_DAY_TRIP_LISTING =
            "In the High Atlas Mountains, we can plan walks of all lengths and difficulties, from short meanders through the lower valleys with mint tea in a Berber home, to multi-day hikes over some of the higher peaks in the region. We can arrange anything from short breaks to longer explorations of the country’s varied regions.";

    private static final String OURIKA_DAY_TRIP_ACTIVITY_SHORT =
            OURIKA_DAY_TRIP_LISTING
                    + " The Ourika Valley is the classic Marrakech escape — easy to reach and rich in Berber culture.";

    private static final String OURIKA_DAY_TRIP_ACTIVITY_FULL =
            OURIKA_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + OURIKA_DAY_TRIP_LISTING
                    + "\n\n"
                    + "On this private-oriented Marrakech day trip, wind up the Ourika road with stops at viewpoints, a walk along the river, optional hike toward the Setti Fatma waterfalls, and lunch in a terrace restaurant or guesthouse. Share mint tea with a local family, browse village stalls for argan and crafts, and return to Marrakech in the evening.";

    /**
     * Copy aligned with https://www.tour-in-morocco.com/tour-destination/marrakech-palm-grove/
     */
    private static final String PALM_GROVE_TOUR_IN_MOROCCO_INTRO = OUZOUD_TOUR_IN_MOROCCO_INTRO;

    /** Main listing idea from the reference “Camel Ride Experience in Marrakech” card (completed for our app). */
    private static final String PALM_GROVE_REFERENCE_LISTING_PARAGRAPH =
            "Experience a camel ride in Marrakech — beyond the red city into the Palmeraie and palm groves. Camel rides here suit visitors who do not have time for the deep desert, and offer a chance to discover the culture of communities living among the palms.";

    private static final String PALM_GROVE_DESTINATION_SHORT = PALM_GROVE_REFERENCE_LISTING_PARAGRAPH;

    private static final String PALM_GROVE_DESTINATION_FULL =
            PALM_GROVE_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + PALM_GROVE_REFERENCE_LISTING_PARAGRAPH
                    + " Stroll or ride through thousands of date palms, pause for mint tea with families who farm this land, and enjoy Atlas views over Marrakech’s famous oasis before returning to the medina.";

    private static final String PALM_GROVE_ACTIVITY_SHORT =
            PALM_GROVE_REFERENCE_LISTING_PARAGRAPH
                    + " Half-day or sunset outings from Marrakech with hotel pick-up — ideal for photos, families, and first-time visitors.";

    private static final String PALM_GROVE_ACTIVITY_FULL =
            PALM_GROVE_TOUR_IN_MOROCCO_INTRO
                    + "\n\n"
                    + "Experience a camel ride in Marrakech: this experience goes beyond expectations to explore the surroundings of the red city. Camel rides in the Palmeraie are recommended for people who do not have time to ride camels in the Sahara, and are an opportunity to discover and learn about the local culture of families living in the oasis.\n\n"
                    + "Typical visits include pick-up in Marrakech, a gentle trek along palm-shaded paths, photo stops with Atlas backdrops, optional mint tea with locals, and return to your hotel — timing can follow morning or golden-hour schedules depending on season.";

    @Override
    @Transactional
    public void run(String... args) {
        if (!runSeedingOnStartup) {
            System.out.println("DataSeeder skipped (app.seeding.run-on-startup=false). PostgreSQL data is not modified by the seeder.");
            return;
        }

        // Only seed users if database is empty
        if (userRepository.count() == 0) {
            seedUsers();
        }
        
        // Destinations: first install (empty DB) gets full defaults including Ouzoud/Ourika once.
        // Existing DB: no core re-seed unless recreate-missing-default-destinations=true.
        long destinationCount = destinationRepository.count();
        if (destinationCount == 0) {
            seedDestinations();
            ensureMarrakechPageCardsIfNeeded();
            seedOuzoudWaterfallsIfNeeded();
            seedOurikaValleyIfNeeded();
            seedMarrakechPalmGroveIfNeeded();
        } else if (recreateMissingDefaultDestinations) {
            seedSaharaDesertIfNeeded();
            ensureMarrakechPageCardsIfNeeded();
            seedOuzoudWaterfallsIfNeeded();
            seedOurikaValleyIfNeeded();
            seedMarrakechPalmGroveIfNeeded();
        } else {
            System.out.println("Skipping core default destination auto-seed (" + destinationCount
                    + " destination(s) in DB). Set app.seeding.recreate-missing-default-destinations=true to re-create missing Marrakech/Sahara/cards.");
        }

        if (destinationCount > 0 && seedCatalogDayTripDestinations) {
            seedOuzoudWaterfallsIfNeeded();
            seedOurikaValleyIfNeeded();
            seedMarrakechPalmGroveIfNeeded();
        }

        List<Destination> destinations = destinationRepository.findAll();
        Destination marrakech = destinations.stream().filter(d -> d.getName().equals("Marrakech")).findFirst().orElse(null);
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);

        if (marrakech == null) {
            if (recreateMissingDefaultDestinations) {
                System.out.println("Marrakech destination not found, creating it...");
                seedDestinations();
                destinations = destinationRepository.findAll();
                marrakech = destinations.stream().filter(d -> d.getName().equals("Marrakech")).findFirst().orElse(null);
            } else {
                System.out.println("Marrakech destination not found — not auto-creating (restore via admin or set app.seeding.recreate-missing-default-destinations=true).");
            }
        }

        if (saharaDesert == null) {
            if (recreateMissingDefaultDestinations) {
                System.out.println("Sahara Desert destination not found, creating it...");
                seedSaharaDesertIfNeeded();
                destinations = destinationRepository.findAll();
                saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
            } else {
                System.out.println("Sahara Desert destination not found — not auto-creating (restore via admin or set app.seeding.recreate-missing-default-destinations=true).");
            }
        }
        
        long activityCount = activityRepository.count();
        System.out.println("Current activity count: " + activityCount);
        
        if (activityCount == 0) {
            System.out.println("No activities found, seeding all activities...");
            if (marrakech == null || saharaDesert == null) {
                System.out.println("ERROR: Cannot seed activities - missing destinations!");
                System.out.println("Marrakech: " + (marrakech != null ? "exists" : "MISSING"));
                System.out.println("Sahara Desert: " + (saharaDesert != null ? "exists" : "MISSING"));
            } else {
                seedActivities();
                // Also seed all the other activities
                System.out.println("Seeding additional activities...");
                seed4DaysTour(saharaDesert);
                seed4DaysDesertTripFromMarrakesh(saharaDesert);
                seed3DaysDesertTripFromMarrakechToFes(saharaDesert);
                seed3DaySaharaDesertTourFromMarrakech(saharaDesert);
                seed2DayDesertTourFromMarrakech(saharaDesert);
                seed12DaysInMoroccoIncludingChefchaouenIfNeeded();
                seed7DaysTourCasablancaToMarrakechIfNeeded();
                System.out.println("Finished seeding activities. New count: " + activityRepository.count());
            }
        } else {
            if (relinkSaharaActivities) {
                System.out.println("Updating Sahara Desert activities destination (app.seeding.relink-sahara-activities=true)...");
                updateSaharaDesertActivitiesDestination();
            }
            // Ensure 4-day tour exists even if other activities are present
            seed4DaysTourIfNeeded();
            // Ensure 4 Days Desert Trip from Marrakesh exists
            seed4DaysDesertTripFromMarrakeshIfNeeded();
            // Ensure 3 Day Sahara Desert Trip from Marrakech to Fes exists
            seed3DaysDesertTripFromMarrakechToFesIfNeeded();
            // Ensure 3 Day Sahara Desert Tour from Marrakech exists
            seed3DaySaharaDesertTourFromMarrakechIfNeeded();
            // Ensure 2 Day Desert Tour from Marrakech exists
            seed2DayDesertTourFromMarrakechIfNeeded();
            seed12DaysInMoroccoIncludingChefchaouenIfNeeded();
            seed7DaysTourCasablancaToMarrakechIfNeeded();
            if (reactivateAllActivities) {
                System.out.println("Re-activating inactive activities (app.seeding.reactivate-all-activities=true)...");
                ensureAllActivitiesAreActive();
            }
        }

        seedOuzoudMarrakechDayTripActivityIfNeeded();
        seedOurikaValleyActivitiesIfNeeded();
        seedMarrakechPalmGroveCamelRideActivityIfNeeded();

        // Only seed other data if database is empty
        if (userRepository.count() > 0 && bookingRepository.count() == 0) {
            seedBookings();
            seedReviews();
            seedFavorites();
            seedSettings();
        } else if (userRepository.count() == 0) {
            seedBookings();
            seedReviews();
            seedFavorites();
            seedSettings();
        }
        
        if (applyTourInMoroccoReference) {
            applyTourInMoroccoSaharaDestinationPageReference();
            applyTourInMoroccoOuzoudDestinationPageReference();
            applyTourInMoroccoMarrakechPalmGroveDestinationPageReference();
        }
        
        // Log summary
        System.out.println("=== Seeder Summary ===");
        System.out.println("Destinations: " + destinationRepository.count());
        System.out.println("Activities: " + activityRepository.count());
        System.out.println("Active Activities: " + activityRepository.findAll().stream()
                .filter(a -> a.getActive() != null && a.getActive()).count());
        List<Destination> allDests = destinationRepository.findAll();
        for (Destination dest : allDests) {
            long destActivityCount = activityRepository.findAll().stream()
                    .filter(a -> a.getDestination() != null && 
                            a.getDestination().getId().equals(dest.getId()) &&
                            a.getActive() != null && a.getActive())
                    .count();
            System.out.println("  - " + dest.getName() + ": " + destActivityCount + " activities");
        }
        System.out.println("=====================");
    }
    
    private void seedUsers() {
        // Admin user
        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@tourisme.com")
                .password(passwordEncoder.encode("admin123"))
                .phone("+1234567890")
                .role(User.Role.ROLE_ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);
        
        // Client users
        List<User> clients = new ArrayList<>(Arrays.asList(
                User.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .email("john.doe@example.com")
                        .password(passwordEncoder.encode("client123"))
                        .phone("+1234567891")
                        .role(User.Role.ROLE_CLIENT)
                        .active(true)
                        .build(),
                User.builder()
                        .firstName("Jane")
                        .lastName("Smith")
                        .email("jane.smith@example.com")
                        .password(passwordEncoder.encode("client123"))
                        .phone("+1234567892")
                        .role(User.Role.ROLE_CLIENT)
                        .active(true)
                        .build(),
                User.builder()
                        .firstName("Ahmed")
                        .lastName("Al-Mansouri")
                        .email("ahmed@example.com")
                        .password(passwordEncoder.encode("client123"))
                        .phone("+971501234567")
                        .role(User.Role.ROLE_CLIENT)
                        .active(true)
                        .build(),
                User.builder()
                        .firstName("Sarah")
                        .lastName("Johnson")
                        .email("sarah.johnson@example.com")
                        .password(passwordEncoder.encode("client123"))
                        .phone("+1234567893")
                        .role(User.Role.ROLE_CLIENT)
                        .active(true)
                        .build(),
                User.builder()
                        .firstName("Mohammed")
                        .lastName("Hassan")
                        .email("mohammed@example.com")
                        .password(passwordEncoder.encode("client123"))
                        .phone("+971502345678")
                        .role(User.Role.ROLE_CLIENT)
                        .active(true)
                        .build()
        ));
        userRepository.saveAll(clients);
    }
    
    private void seedDestinations() {
        // Seed Marrakech
        String slug = SlugUtil.generateSlug("Marrakech");
        Destination destination = destinationRepository.findBySlug(slug).orElse(null);
        
        if (destination != null) {
            // Destination already exists - DO NOT UPDATE - preserve all user modifications (images, descriptions, etc.)
            System.out.println("Marrakech destination already exists - preserving all user data");
            return; // Skip saving to preserve existing data
        } else {
            // Create new destination
            destination = Destination.builder()
                    .name("Marrakech")
                    .slug(slug)
                    .shortDescription("Discover the vibrant Red City with its bustling souks and historic palaces")
                    .fullDescription("Marrakech, the Red City, is a feast for the senses. Wander through the maze-like medina, visit the stunning Bahia Palace, experience the vibrant Jemaa el-Fnaa square, and indulge in authentic Moroccan cuisine. The home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces.")
                    .imageUrl("https://images.unsplash.com/photo-1539650116574-75c0c6d73a6e?w=1200")
                    .country("Morocco")
                    .city("Marrakech")
                    .featured(true)
                    .build();
        }
        
        destinationRepository.save(destination);
        System.out.println("Created Marrakech destination");
        ensureMarrakechDefaultPageCards(destination);

        // Seed Sahara Desert
        String saharaSlug = SlugUtil.generateSlug("Sahara Desert");
        Destination saharaDestination = destinationRepository.findBySlug(saharaSlug).orElse(null);
        
        if (saharaDestination != null) {
            // Destination already exists - DO NOT UPDATE - preserve all user modifications (images, descriptions, etc.)
            System.out.println("Sahara Desert destination already exists - preserving all user data");
            return; // Skip saving to preserve existing data
        } else {
            // Create new destination
            saharaDestination = Destination.builder()
                    .name("Sahara Desert")
                    .slug(saharaSlug)
                    .shortDescription("Shared Sahara Desert tours — Merzouga dunes, UNESCO kasbahs, camel rides, and nights under the stars.")
                    .fullDescription("The people of this country are super friendly and welcoming. We invite you to explore the pristine beauty of Morocco — Sahara, mountains, and beaches — with tailor-made trip packages and Sahara desert tours.\n\n"
                            + "The Sahara Desert of Morocco offers Merzouga and Erg Chebbi, camel rides, luxury desert camps, and UNESCO kasbahs from the High Atlas to the golden dunes.")
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .country("Morocco")
                    .city("Merzouga")
                    .featured(true)
                    .build();
            destinationRepository.save(saharaDestination);
            System.out.println("Created Sahara Desert destination");
        }
    }
    
    private void seedSaharaDesertIfNeeded() {
        String saharaSlug = SlugUtil.generateSlug("Sahara Desert");
        Destination saharaDestination = destinationRepository.findBySlug(saharaSlug).orElse(null);
        
        if (saharaDestination == null) {
            // Create Sahara Desert destination if it doesn't exist - never update existing
            saharaDestination = Destination.builder()
                    .name("Sahara Desert")
                    .slug(saharaSlug)
                    .shortDescription("Shared Sahara Desert tours — Merzouga dunes, UNESCO kasbahs, camel rides, and nights under the stars.")
                    .fullDescription("The people of this country are super friendly and welcoming. We invite you to explore the pristine beauty of Morocco — Sahara, mountains, and beaches — with tailor-made trip packages and Sahara desert tours.\n\n"
                            + "The Sahara Desert of Morocco is one of the most extraordinary destinations in the world: sand dunes of Merzouga and Erg Chebbi, camel rides at sunset, nights in Berber camps, and UNESCO World Heritage kasbahs. Cross the High Atlas, discover palm groves and fortified ksour, and soak up the hospitality of multi-lingual locals on shared and private itineraries inspired by the classic Sahara listings.")
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .country("Morocco")
                    .city("Merzouga")
                    .featured(true)
                    .build();
            destinationRepository.save(saharaDestination);
            System.out.println("Created Sahara Desert destination");
        } else {
            System.out.println("Sahara Desert destination already exists, preserving existing data");
        }
    }

    /**
     * Ensures the Ouzoud Waterfalls destination exists (Tour in Morocco–style day trip from Marrakech).
     * Never overwrites an existing row with the same slug.
     */
    private void seedOuzoudWaterfallsIfNeeded() {
        String slug = SlugUtil.generateSlug("Ouzoud Waterfalls");
        Destination ouzoud = destinationRepository.findBySlug(slug).orElse(null);

        if (ouzoud == null) {
            ouzoud = Destination.builder()
                    .name("Ouzoud Waterfalls")
                    .slug(slug)
                    .shortDescription(OUZOUD_DESTINATION_SHORT)
                    .fullDescription(OUZOUD_DESTINATION_FULL)
                    .imageUrl("https://images.unsplash.com/photo-1584466977763-009977e7c8b6?w=1200")
                    .country("Morocco")
                    .city("Ouzoud")
                    .featured(true)
                    .build();
            destinationRepository.save(ouzoud);
            System.out.println("Created Ouzoud Waterfalls destination");
        } else {
            System.out.println("Ouzoud Waterfalls destination already exists, preserving existing data");
        }
        ensureOuzoudWaterfallsPageCards(ouzoud);
    }

    /**
     * Adds default “Highlights” page cards (same shape as destination detail {@code pageCards}: image, title, body)
     * when none exist yet — does not replace admin-configured cards.
     */
    private void ensureOuzoudWaterfallsPageCards(Destination destination) {
        List<DestinationPageCard> existing =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(destination.getId());
        if (!existing.isEmpty()) {
            return;
        }

        DestinationPageCard card1 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(0)
                .imageUrl("https://images.unsplash.com/photo-1584466977763-009977e7c8b6?w=1200")
                .title("Marrakech Day Trip to Ouzoud Waterfalls")
                .body(OUZOUD_REFERENCE_LISTING_PARAGRAPH)
                .build();
        card1.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card1)
                .languageCode("fr")
                .title("Excursion d’une journée Marrakech – cascades d’Ouzoud")
                .body("À 150 km de Marrakech, les cascades d’Ouzoud forment un paysage à couper le souffle. Ce site compte parmi les plus beaux du Maroc ! Au cœur d’une végétation luxuriante, trois chutes se succèdent, la plus haute atteignant 110 m. Après 1 h 30 de route, vous arrivez à Ouzoud.")
                .build());

        DestinationPageCard card2 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(1)
                .imageUrl("https://images.unsplash.com/photo-1505142468610-359e7d316be0?w=1200")
                .title("Three waterfalls, dramatic drops")
                .body("The site is famous for its tiered cascades; the highest single drop is about 110 metres. Wooden walkways and viewpoints line the gorge — expect mist, rainbows on sunny days, and unforgettable panoramas.")
                .build();
        card2.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card2)
                .languageCode("fr")
                .title("Trois cascades, chutes spectaculaires")
                .body("Le site est célèbre pour ses cascades en gradins ; la chute la plus haute atteint environ 110 m. Des passerelles et des belvédères longent les gorges — brume, arcs-en-ciel par beau temps et vues inoubliables.")
                .build());

        DestinationPageCard card3 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(2)
                .imageUrl("https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200")
                .title("Green gorges & Barbary macaques")
                .body("Olive groves and lush cliffs frame the falls. Wild Barbary macaques live in the area — keep snacks sealed and enjoy watching them from a distance. Small cafés and boat rides add to a classic Moroccan nature outing.")
                .build();
        card3.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card3)
                .languageCode("fr")
                .title("Gorges verdoyantes & magots")
                .body("Oliviers et falaises verdoyantes encadrent les chutes. Des magots berbères vivent sur place — gardez les encas bien fermés et observez-les de loin. Petits cafés et barques complètent une sortie nature marocaine typique.")
                .build());

        destination.getPageCards().add(card1);
        destination.getPageCards().add(card2);
        destination.getPageCards().add(card3);
        destinationRepository.save(destination);
        System.out.println("Seeded Ouzoud Waterfalls destination page cards (highlights)");
    }

    private void seedOuzoudMarrakechDayTripActivityIfNeeded() {
        String actSlug = SlugUtil.generateSlug("Marrakech Day Trip to Ouzoud Waterfalls");
        if (activityRepository.findBySlug(actSlug).isPresent()) {
            return;
        }
        Destination ouzoud = destinationRepository.findBySlug(SlugUtil.generateSlug("Ouzoud Waterfalls")).orElse(null);
        if (ouzoud == null) {
            return;
        }

        Activity activity = Activity.builder()
                .title("Marrakech Day Trip to Ouzoud Waterfalls")
                .slug(actSlug)
                .shortDescription(OUZOUD_ACTIVITY_SHORT)
                .fullDescription(OUZOUD_ACTIVITY_FULL)
                .price(new BigDecimal("49.00"))
                .premiumPrice(new BigDecimal("75.00"))
                .budgetPrice(new BigDecimal("49.00"))
                .duration("1 Day")
                .location("Marrakech – Ouzoud Waterfalls")
                .category("Marrakech Day Trips")
                .tourType(Activity.TourType.SHARED)
                .difficultyLevel(Activity.DifficultyLevel.EASY)
                .ratingAverage(new BigDecimal("4.8"))
                .reviewCount(128)
                .featured(true)
                .active(true)
                .maxGroupSize(17)
                .availableSlots(60)
                .imageUrl("https://images.unsplash.com/photo-1584466977763-009977e7c8b6?w=1200")
                .galleryImages(new ArrayList<>(Arrays.asList(
                        "https://images.unsplash.com/photo-1584466977763-009977e7c8b6?w=1200",
                        "https://images.unsplash.com/photo-1505142468610-359e7d316be0?w=1200",
                        "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1200"
                )))
                .availability("Daily")
                .departureLocation("Marrakech")
                .returnLocation("Marrakech")
                .meetingTime("Hotel pick-up in Marrakech (approximately 8:00 AM)")
                .whatToExpect(OUZOUD_ACTIVITY_SHORT)
                .includedItems(new ArrayList<>(Arrays.asList(
                        "Round-trip transport from Marrakech",
                        "Professional driver / guide",
                        "Free time to explore the waterfalls and viewpoints"
                )))
                .excludedItems(new ArrayList<>(Arrays.asList(
                        "Lunch and drinks",
                        "Optional boat ride at the falls",
                        "Personal expenses",
                        "Tips and gratuities"
                )))
                .complementaries(new ArrayList<>(Arrays.asList(
                        "Comfortable walking shoes",
                        "Sunscreen and hat",
                        "Camera"
                )))
                .itinerary(new ArrayList<>(Arrays.asList(
                        "Morning: Pick-up in Marrakech and scenic drive (~1h30) toward the Middle Atlas.",
                        "At Ouzoud: Walk to the upper viewpoints, descend toward the pools; optional lunch and boat ride.",
                        "Afternoon: Last photos and return transfer to Marrakech."
                )))
                .destination(ouzoud)
                .build();

        activityRepository.save(activity);
        System.out.println("Created Marrakech Day Trip to Ouzoud Waterfalls activity (Tour in Morocco listing).");
    }

    /**
     * Ensures the Ourika Valley destination exists (Tour in Morocco–style Atlas / day trips from Marrakech).
     */
    private void seedOurikaValleyIfNeeded() {
        String slug = SlugUtil.generateSlug("Ourika Valley");
        Destination ourika = destinationRepository.findBySlug(slug).orElse(null);

        if (ourika == null) {
            ourika = Destination.builder()
                    .name("Ourika Valley")
                    .slug(slug)
                    .shortDescription(OURIKA_DESTINATION_SHORT)
                    .fullDescription(OURIKA_DESTINATION_FULL)
                    .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                    .country("Morocco")
                    .city("Ourika Valley")
                    .featured(true)
                    .build();
            destinationRepository.save(ourika);
            System.out.println("Created Ourika Valley destination");
        } else {
            System.out.println("Ourika Valley destination already exists, preserving existing data");
        }
        ensureOurikaValleyPageCards(ourika);
    }

    private void ensureOurikaValleyPageCards(Destination destination) {
        List<DestinationPageCard> existing =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(destination.getId());
        if (!existing.isEmpty()) {
            return;
        }

        DestinationPageCard card1 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(0)
                .imageUrl("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200")
                .title("High Atlas Mountains and 3 Valleys Day Trip")
                .body(OURIKA_3_VALLEYS_LISTING)
                .build();
        card1.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card1)
                .languageCode("fr")
                .title("Haut Atlas et trois vallées — journée d’aventure")
                .body("Les montagnes de l’Atlas sont en fait trois chaînes distinctes qui traversent l’intérieur du Maroc en bandes, le divisant en zones plus basses. Au nord le Moyen Atlas, au sud l’Anti-Atlas qui freine le Sahara occidental.")
                .build());

        DestinationPageCard card2 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(1)
                .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                .title("Marrakech Day Trip to Ourika Valley")
                .body(OURIKA_DAY_TRIP_LISTING)
                .build();
        card2.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card2)
                .languageCode("fr")
                .title("Excursion d’une journée Marrakech – vallée de l’Ourika")
                .body("Dans le Haut Atlas, nous pouvons organiser des marches de toutes durées et difficultés, des petites balades dans les vallées avec thé à la menthe chez l’habitant, aux randonnées de plusieurs jours sur les sommets.")
                .build());

        DestinationPageCard card3 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(2)
                .imageUrl("https://images.unsplash.com/photo-1449452198869-9c768336ebc6?w=1200")
                .title("Berber villages & the Ourika River")
                .body("Follow the river past orchards and terraced gardens, stop in adobe villages for tea and crafts, and breathe cool mountain air less than an hour from Marrakech — Morocco’s favourite day-trip escape into the Atlas.")
                .build();
        card3.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card3)
                .languageCode("fr")
                .title("Villages berbères & rivière de l’Ourika")
                .body("Suivez la rivière entre vergers et jardins en terrasses, visitez des villages en pisé pour le thé et l’artisanat, et respirez l’air frais de la montagne à moins d’une heure de Marrakech.")
                .build());

        destination.getPageCards().add(card1);
        destination.getPageCards().add(card2);
        destination.getPageCards().add(card3);
        destinationRepository.save(destination);
        System.out.println("Seeded Ourika Valley destination page cards (highlights)");
    }

    private void seedOurikaValleyActivitiesIfNeeded() {
        Destination ourika = destinationRepository.findBySlug(SlugUtil.generateSlug("Ourika Valley")).orElse(null);
        if (ourika == null) {
            return;
        }

        String slug3 = SlugUtil.generateSlug("High Atlas Mountains and 3 Valleys Day Trip");
        if (activityRepository.findBySlug(slug3).isEmpty()) {
            Activity a3 = Activity.builder()
                    .title("High Atlas Mountains and 3 Valleys Day Trip")
                    .slug(slug3)
                    .shortDescription(OURIKA_3_VALLEYS_ACTIVITY_SHORT)
                    .fullDescription(OURIKA_3_VALLEYS_ACTIVITY_FULL)
                    .price(new BigDecimal("59.00"))
                    .premiumPrice(new BigDecimal("89.00"))
                    .budgetPrice(new BigDecimal("59.00"))
                    .duration("1 Day")
                    .location("Marrakech – High Atlas / Ourika valleys")
                    .category("Adventure Trip")
                    .tourType(Activity.TourType.SHARED)
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.7"))
                    .reviewCount(96)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(48)
                    .imageUrl("https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200",
                            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200"
                    )))
                    .availability("Daily")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("Hotel pick-up in Marrakech (approximately 8:30 AM)")
                    .whatToExpect(OURIKA_3_VALLEYS_ACTIVITY_SHORT)
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Round-trip transport from Marrakech",
                            "Driver / guide",
                            "Photo stops and scenic routing"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and drinks",
                            "Personal expenses",
                            "Tips and gratuities"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes",
                            "Layers for mountain weather",
                            "Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Morning: pick-up in Marrakech and drive into the High Atlas foothills.",
                            "Midday: cross contrasting valleys with viewpoints and short walks.",
                            "Afternoon: return to Marrakech with stops as time allows."
                    )))
                    .destination(ourika)
                    .build();
            activityRepository.save(a3);
            System.out.println("Created High Atlas Mountains and 3 Valleys Day Trip activity (Tour in Morocco listing).");
        }

        String slugD = SlugUtil.generateSlug("Marrakech Day Trip to Ourika Valley");
        if (activityRepository.findBySlug(slugD).isEmpty()) {
            Activity ad = Activity.builder()
                    .title("Marrakech Day Trip to Ourika Valley")
                    .slug(slugD)
                    .shortDescription(OURIKA_DAY_TRIP_ACTIVITY_SHORT)
                    .fullDescription(OURIKA_DAY_TRIP_ACTIVITY_FULL)
                    .price(new BigDecimal("79.00"))
                    .premiumPrice(new BigDecimal("120.00"))
                    .budgetPrice(new BigDecimal("79.00"))
                    .duration("1 Day")
                    .location("Marrakech – Ourika Valley")
                    .category("Private Trip")
                    .tourType(Activity.TourType.PRIVATE)
                    .difficultyLevel(Activity.DifficultyLevel.EASY)
                    .ratingAverage(new BigDecimal("4.9"))
                    .reviewCount(142)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(6)
                    .availableSlots(40)
                    .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200",
                            "https://images.unsplash.com/photo-1449452198869-9c768336ebc6?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200"
                    )))
                    .availability("Daily")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("Flexible pick-up from your Marrakech hotel or riad")
                    .whatToExpect(OURIKA_DAY_TRIP_ACTIVITY_SHORT)
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Private vehicle and driver",
                            "Flexible pacing and stops",
                            "Berber home visit for mint tea (when available)"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and drinks",
                            "Optional hike guides",
                            "Tips and gratuities"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Sun hat",
                            "Walking shoes",
                            "Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Drive from Marrakech along the Ourika valley road with photo stops.",
                            "Walk the river, visit villages, optional ascent toward Setti Fatma waterfalls.",
                            "Lunch on a terrace; return to Marrakech in the late afternoon."
                    )))
                    .destination(ourika)
                    .build();
            activityRepository.save(ad);
            System.out.println("Created Marrakech Day Trip to Ourika Valley activity (Tour in Morocco listing).");
        }
    }

    /**
     * Ensures the Marrakech Palm Grove destination exists (Tour in Morocco–style Palmeraie / camel listings).
     */
    private void seedMarrakechPalmGroveIfNeeded() {
        String slug = SlugUtil.generateSlug("Marrakech Palm Grove");
        Destination palmGrove = destinationRepository.findBySlug(slug).orElse(null);

        if (palmGrove == null) {
            palmGrove = Destination.builder()
                    .name("Marrakech Palm Grove")
                    .slug(slug)
                    .shortDescription(PALM_GROVE_DESTINATION_SHORT)
                    .fullDescription(PALM_GROVE_DESTINATION_FULL)
                    .imageUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200")
                    .country("Morocco")
                    .city("Marrakech")
                    .featured(true)
                    .build();
            destinationRepository.save(palmGrove);
            System.out.println("Created Marrakech Palm Grove destination");
        } else {
            System.out.println("Marrakech Palm Grove destination already exists, preserving existing data");
        }
        ensureMarrakechPalmGrovePageCards(palmGrove);
    }

    private void ensureMarrakechPalmGrovePageCards(Destination destination) {
        List<DestinationPageCard> existing =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(destination.getId());
        if (!existing.isEmpty()) {
            return;
        }

        DestinationPageCard card1 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(0)
                .imageUrl("https://images.unsplash.com/photo-1591608971362-6246c5fd30ef?w=1200")
                .title("Camel Ride Experience in Marrakech")
                .body(PALM_GROVE_REFERENCE_LISTING_PARAGRAPH)
                .build();
        card1.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card1)
                .languageCode("fr")
                .title("Balade à dos de chameau à Marrakech")
                .body("Vivez une balade à dos de chameau à Marrakech — au-delà de la ville rouge vers la Palmeraie et les palmeraies. Idéal si vous n’avez pas le temps du désert : découverte de la culture des oasis et des familles qui y vivent.")
                .build());

        DestinationPageCard card2 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(1)
                .imageUrl("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200")
                .title("The Palmeraie — Marrakech’s palm oasis")
                .body("Thousands of date palms spread north of the medina: shady tracks, kasbah-style guesthouses, and views of the High Atlas. Short drives from the centre make this Morocco’s easiest “desert feel” without leaving Marrakech.")
                .build();
        card2.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card2)
                .languageCode("fr")
                .title("La Palmeraie — l’oasis de Marrakech")
                .body("Des milliers de palmiers s’étendent au nord de la médina : sentiers ombragés, maisons d’hôtes de style kasbah et vues sur le Haut Atlas. Un court trajet depuis le centre pour une ambiance désert sans quitter Marrakech.")
                .build());

        DestinationPageCard card3 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(2)
                .imageUrl("https://images.unsplash.com/photo-1542401886-65d6c61db217?w=1200")
                .title("Culture & crafts in the groves")
                .body("Meet Berber families, taste fresh dates and mint tea, and browse small cooperatives — the Palmeraie blends nature with living heritage on the edge of the red city.")
                .build();
        card3.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card3)
                .languageCode("fr")
                .title("Culture et artisanat dans les palmeraies")
                .body("Rencontres avec des familles berbères, dattes et thé à la menthe, petites coopératives — la Palmeraie mêle nature et patrimoine vivant aux portes de la ville rouge.")
                .build());

        destination.getPageCards().add(card1);
        destination.getPageCards().add(card2);
        destination.getPageCards().add(card3);
        destinationRepository.save(destination);
        System.out.println("Seeded Marrakech Palm Grove destination page cards (highlights)");
    }

    private void seedMarrakechPalmGroveCamelRideActivityIfNeeded() {
        String actSlug = SlugUtil.generateSlug("Camel Ride Experience in Marrakech");
        if (activityRepository.findBySlug(actSlug).isPresent()) {
            return;
        }
        Destination palmGrove = destinationRepository.findBySlug(SlugUtil.generateSlug("Marrakech Palm Grove")).orElse(null);
        if (palmGrove == null) {
            return;
        }

        Activity activity = Activity.builder()
                .title("Camel Ride Experience in Marrakech")
                .slug(actSlug)
                .shortDescription(PALM_GROVE_ACTIVITY_SHORT)
                .fullDescription(PALM_GROVE_ACTIVITY_FULL)
                .price(new BigDecimal("45.00"))
                .premiumPrice(new BigDecimal("69.00"))
                .budgetPrice(new BigDecimal("45.00"))
                .duration("Half Day")
                .location("Marrakech – Palmeraie")
                .category("Marrakech Day Trips")
                .tourType(Activity.TourType.SHARED)
                .difficultyLevel(Activity.DifficultyLevel.EASY)
                .ratingAverage(new BigDecimal("4.7"))
                .reviewCount(94)
                .featured(true)
                .active(true)
                .maxGroupSize(14)
                .availableSlots(55)
                .imageUrl("https://images.unsplash.com/photo-1591608971362-6246c5fd30ef?w=1200")
                .galleryImages(new ArrayList<>(Arrays.asList(
                        "https://images.unsplash.com/photo-1591608971362-6246c5fd30ef?w=1200",
                        "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200",
                        "https://images.unsplash.com/photo-1542401886-65d6c61db217?w=1200"
                )))
                .availability("Daily")
                .departureLocation("Marrakech")
                .returnLocation("Marrakech")
                .meetingTime("Hotel pick-up in Marrakech (morning or late afternoon — confirm locally)")
                .whatToExpect(PALM_GROVE_ACTIVITY_SHORT)
                .includedItems(new ArrayList<>(Arrays.asList(
                        "Round-trip transport from Marrakech (typical Palmeraie circuits)",
                        "Camel trek in the palm groves",
                        "Local guide / camel handlers"
                )))
                .excludedItems(new ArrayList<>(Arrays.asList(
                        "Drinks and snacks unless specified",
                        "Tips and gratuities",
                        "Personal expenses"
                )))
                .complementaries(new ArrayList<>(Arrays.asList(
                        "Sunscreen and hat",
                        "Comfortable closed shoes",
                        "Camera"
                )))
                .itinerary(new ArrayList<>(Arrays.asList(
                        "Pick-up in Marrakech and short drive to the Palmeraie.",
                        "Camel ride through palm groves with photo stops and optional tea break.",
                        "Return transfer to your accommodation in Marrakech."
                )))
                .destination(palmGrove)
                .build();

        activityRepository.save(activity);
        System.out.println("Created Camel Ride Experience in Marrakech activity (Tour in Morocco listing).");
    }

    private void ensureMarrakechPageCardsIfNeeded() {
        String slug = SlugUtil.generateSlug("Marrakech");
        destinationRepository.findBySlug(slug).ifPresent(this::ensureMarrakechDefaultPageCards);
    }

    /**
     * Same highlights card layout as {@link #ensureOuzoudWaterfallsPageCards} for the primary seeded destination.
     */
    private void ensureMarrakechDefaultPageCards(Destination destination) {
        List<DestinationPageCard> existing =
                destinationPageCardRepository.findByDestinationIdOrderBySortOrderAsc(destination.getId());
        if (!existing.isEmpty()) {
            return;
        }

        DestinationPageCard card1 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(0)
                .imageUrl("https://images.unsplash.com/photo-1597212618440-806262de4f6b?w=1200")
                .title("Medina & Jemaa el-Fnaa")
                .body("Lose yourself in the UNESCO-listed medina: narrow lanes, artisan workshops, and the legendary Jemaa el-Fnaa square — storytellers, musicians, and food stalls from dusk till late night.")
                .build();
        card1.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card1)
                .languageCode("fr")
                .title("Médina & place Jemaa el-Fnaa")
                .body("Perdez-vous dans la médina classée UNESCO : ruelles, ateliers d’artisans et la légendaire place Jemaa el-Fnaa — conteurs, musiciens et stands de street food du crépuscule à la nuit.")
                .build());

        DestinationPageCard card2 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(1)
                .imageUrl("https://images.unsplash.com/photo-1553603227-2358aabe821e?w=1200")
                .title("Palaces, riads & gardens")
                .body("From Bahia Palace’s tiled courtyards to the blue brilliance of Majorelle, Marrakech blends Andalusian, Berber, and French influences. Stay in a riad, sip mint tea on a rooftop, and soak up the Red City’s architecture.")
                .build();
        card2.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card2)
                .languageCode("fr")
                .title("Palais, riads & jardins")
                .body("Des cours du palais Bahia au bleu éclatant du jardin Majorelle, Marrakech mêle influences andalouses, berbères et françaises. Nuitée en riad, thé à la menthe sur les toits et architecture de la Ville rouge.")
                .build());

        DestinationPageCard card3 = DestinationPageCard.builder()
                .destination(destination)
                .sortOrder(2)
                .imageUrl("https://images.unsplash.com/photo-1489749798305-4fea3ae3d66e?w=1200")
                .title("Souks, crafts & cuisine")
                .body("Haggle (politely) for carpets, lanterns, and spices; watch leather and metalwork being made. Tagines, pastilla, and orange-blossom pastries reward every walk — a full sensory introduction to Morocco.")
                .build();
        card3.getTranslations().add(DestinationPageCardTranslation.builder()
                .card(card3)
                .languageCode("fr")
                .title("Souks, artisanat & cuisine")
                .body("Marchandez (avec le sourire) tapis, lanternes et épices ; observez le travail du cuir et du métal. Tagines, pastilla et pâtisseries à l’eau de fleur d’oranger — une immersion sensorielle au Maroc.")
                .build());

        destination.getPageCards().add(card1);
        destination.getPageCards().add(card2);
        destination.getPageCards().add(card3);
        destinationRepository.save(destination);
        System.out.println("Seeded Marrakech destination page cards (highlights)");
    }

    private void updateSaharaDesertActivitiesDestination() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            System.out.println("Sahara Desert destination not found, creating it...");
            seedSaharaDesertIfNeeded();
            saharaDesert = destinationRepository.findAll().stream()
                    .filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
            if (saharaDesert == null) {
                System.out.println("Failed to create Sahara Desert destination");
                return;
            }
        }
        
        // Find all activities - check by category, title, or description
        List<Activity> allActivities = activityRepository.findAll();
        List<Activity> saharaActivities = allActivities.stream()
                .filter(a -> {
                    // Check if it's a Sahara Desert tour by category, title, or description
                    boolean isSaharaTour = false;
                    if (a.getCategory() != null && a.getCategory().contains("Sahara Desert")) {
                        isSaharaTour = true;
                    } else if (a.getTitle() != null && (
                            a.getTitle().contains("Sahara") || 
                            a.getTitle().contains("Desert") ||
                            a.getTitle().contains("Merzouga"))) {
                        isSaharaTour = true;
                    } else if (a.getShortDescription() != null && (
                            a.getShortDescription().contains("Sahara") ||
                            a.getShortDescription().contains("desert"))) {
                        isSaharaTour = true;
                    }
                    return isSaharaTour;
                })
                .collect(Collectors.toList());
        
        System.out.println("Found " + saharaActivities.size() + " Sahara Desert activities to update");
        
        if (saharaActivities.isEmpty()) {
            return;
        }
        
        boolean updated = false;
        for (Activity activity : saharaActivities) {
            boolean needsUpdate = false;
            
            // Only update critical fields - preserve all user modifications (images, descriptions, prices, etc.)
            if (relinkSaharaActivities) {
                if (activity.getDestination() == null) {
                    activity.setDestination(saharaDesert);
                    needsUpdate = true;
                    System.out.println("Updating activity " + activity.getTitle() + " - setting destination to Sahara Desert (preserving user modifications)");
                } else if (!activity.getDestination().getName().equals("Sahara Desert")) {
                    activity.setDestination(saharaDesert);
                    needsUpdate = true;
                    System.out.println("Updating activity " + activity.getTitle() + " - changing destination from " + 
                            activity.getDestination().getName() + " to Sahara Desert (preserving user modifications)");
                }
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsUpdate = true;
                System.out.println("Activating activity " + activity.getTitle() + " (preserving user modifications)");
            }
            
            if (needsUpdate) {
                updated = true;
            }
        }
        
        if (updated) {
            activityRepository.saveAll(saharaActivities);
            System.out.println("Updated critical fields for " + saharaActivities.size() + " Sahara Desert activities (user modifications preserved)");
        } else {
            System.out.println("No Sahara Desert activities needed updating");
        }
    }
    
    private void ensureAllActivitiesAreActive() {
        List<Activity> inactiveActivities = activityRepository.findAll().stream()
                .filter(a -> a.getActive() == null || !a.getActive())
                .collect(Collectors.toList());
        
        if (!inactiveActivities.isEmpty()) {
            System.out.println("Found " + inactiveActivities.size() + " inactive activities, activating them (preserving all other data)...");
            for (Activity activity : inactiveActivities) {
                // Only update active status - preserve everything else
                activity.setActive(true);
            }
            activityRepository.saveAll(inactiveActivities);
            System.out.println("Activated " + inactiveActivities.size() + " activities (user modifications preserved)");
        }
    }
    
    private void seedActivities() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination marrakech = destinations.stream().filter(d -> d.getName().equals("Marrakech")).findFirst().orElse(null);
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (marrakech == null) {
            System.out.println("ERROR: Marrakech destination not found! Cannot seed activities.");
            return;
        }
        
        if (saharaDesert == null) {
            System.out.println("ERROR: Sahara Desert destination not found! Creating it now...");
            seedSaharaDesertIfNeeded();
            destinations = destinationRepository.findAll();
            saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
            if (saharaDesert == null) {
                System.out.println("ERROR: Failed to create Sahara Desert destination! Cannot seed activities.");
                return;
            }
        }
        
        System.out.println("Seeding activities with Marrakech and Sahara Desert destinations...");
        
        // Check if activity already exists
        String slug = SlugUtil.generateSlug("Tour from Fes to Marrakech 3 Days");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - only update critical fields (destination, active status)
            // Preserve all user modifications (images, descriptions, prices, etc.)
            activity = existingActivity.get();
            boolean needsSave = false;
            
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            
            if (needsSave) {
                activityRepository.save(activity);
                System.out.println("✓ Updated critical fields for existing activity: " + activity.getTitle() + " (preserved user modifications)");
            } else {
                System.out.println("✓ Activity already exists: " + activity.getTitle() + " (no changes needed)");
            }
        } else {
            // Create new activity
            activity = Activity.builder()
                    .title("Tour from Fes to Marrakech 3 Days")
                    .slug(slug)
                    .shortDescription("Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures")
                    .fullDescription("During this Tour from Fes to Marrakech 3 Days, you will explore the imperial cities, high atlas mountains, southern market town renowned Unesco Heritage site, or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Sahara Desert Tours to the Merzouga and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals. Do you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty. You will fully enjoy the pleasure of roaming freely on some of the most beautiful slopes and enjoy the beautiful sunset in the Sahara. A short but full impression of this area of Morocco with its Mountains, gorges, valleys, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the sunrise, and the hospitality of the people.")
                    .price(new BigDecimal("140.00"))
                    .duration("3 Days")
                    .location("Fes to Marrakech")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.7"))
                    .reviewCount(52)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(32)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Fes")
                    .returnLocation("Marrakech")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("A Tour from Fes to Marrakech is an unforgettable experience that will allow you to go through the history and the hidden culture and traditions of the daily life of Morocco as well as the discovery of the old imperial cities in Morocco. Do you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty. You will fully enjoy the pleasure of roaming freely on some of the most beautiful slopes and enjoy the beautiful sunset in the Sahara. A short but full impression of this area of Morocco with its Mountains, gorges, green palm groves, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the camel ride experience, and the hospitality of the people.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: Fes - Azrou - Ziz Valley - Sahara Desert. Your driver/guide will pick you up at your Fes accommodation around 8 am for a beautiful journey to the beautiful dunes of Merzouga, passing by Ifran known as \"the Switzerland Moroccan\", one hour drive, we will stop to visit the cedar forest in Azrou and see the monkeys climbing cedar trees. Then follow the path and cross the mountains of the Middle Atlas, passing by Midelt, stop at Ziz valley for pictures of stunning panoramic views over the gorges of the Ziz River, next to the city Erfoud of famous by fossil where are extracted the rocks containing fossils, Arrival at Merzouga dunes \"Erg Chebbi\" where we can feel the peace and tranquility of the desert. You will be welcomed by a mint tea enjoying the silence of the Sahara. And then, get ready for a new adventure experience, to a Berber camp in a lifetime experience, you will ride a camel through the golden dunes of Erg Chebbi, to assist to a wonderful sunset and spend a romantic night in the desert camp. After dinner, we gather around the fire and enjoy the desert night, perhaps with traditional Berber drums.",
                            "Day 2: Sahara Desert - Todra Gorges - Dades Valley - Ouarzazate. Early wake up for a lifetime sunrise over the dunes, you will enjoy the change of sand color with the light of the sun. Prepare your cameras and your phones to take some memorable pictures. After taking photos, you will take shower and breakfast, and then, you will ride camels back to the hotel enjoying the silence and the charm of the dunes with the sunlight. Our Tour from Fes to Marrakech continues for a new journey through the palm groves and oasis direction to Tinghir and its amazing Todra Gorges and the big canyons 300 m high. After the visit, we will continue to Dades gorges above the valley. Many stops for photos along the valley, Then continue to Kalaat Magouna (Rose Valley) famous for its annual festival of roses that takes place every year in May. After visiting a female cooperative of roses, we will drive to Ouarzazate through the oasis of Skoura, where we will stop to visit the old Kasbah Amrideil (500 years old) then arrive in Ouarzazate. Dinner and overnight at the hotel.",
                            "Day 3: Ouarzazate - Kasbah Ait Benhaddou - High Atlas - Marrakech. Breakfast at your Hotel or Riad in Ouarzazate, then, we will start a new journey of our Tour from Fes to Marrakech, our first stop for today is to visit the city's famous kasbah, including that of Taourirt marked as world heritage by Unesco and one of the beautiful palaces in the area. Not far from the city Ouarzazate Kasbah we will stop to visit the museum of movies and cinema studios, where many international movies are shot, like: (Gladiator, Lawrence d'Arabie, Games of the throne, the Babel…). 20 km later we will turn right to visit the famous kasbah of Ait Ben Haddou, after the visit we will continue crossing the high Atlas Mountains and its dramatic landscapes and the hidden Berber villages settled in the foothills of the massive valleys. (optional: we can have lunch in a local restaurant in the Tichka pass) else, we will continue to Marrakech if you want to use your left time visiting the amazing Majorelle gardens."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(21)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.PRIVATE)
                    .build();
        }
        
        activityRepository.save(activity);
        System.out.println("✓ Created activity: " + activity.getTitle() + " (ID: " + activity.getId() + ")");
        
        // Add related activities (Shared Sahara Desert Tours)
        Activity relatedActivity1 = Activity.builder()
                .title("10 Days Morocco Tour from Tangier")
                .slug(SlugUtil.generateSlug("10 Days Morocco Tour from Tangier"))
                .shortDescription("Explore Morocco in 10 days starting from Tangier")
                .fullDescription("A comprehensive 10-day journey through Morocco starting from Tangier, exploring the imperial cities, Sahara Desert, mountains, and coastal regions.")
                .price(new BigDecimal("99.00"))
                .duration("10 Days")
                .location("Tangier to Morocco")
                .category("Shared Sahara Desert Tours")
                .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                .ratingAverage(new BigDecimal("4.8"))
                .reviewCount(45)
                .featured(true)
                .active(true)
                .maxGroupSize(16)
                .availableSlots(32)
                .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                .galleryImages(new ArrayList<>(Arrays.asList(
                        "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                        "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200"
                )))
                .availability("Everyday")
                .departureLocation("Tangier")
                .returnLocation("Tangier")
                .meetingTime("8 AM at your accommodation")
                .whatToExpect("A comprehensive 10-day journey through Morocco exploring imperial cities, Sahara Desert, and coastal regions.")
                .includedItems(new ArrayList<>(Arrays.asList(
                        "Transportation in shared vehicle",
                        "Professional driver/guide",
                        "Hotel accommodation",
                        "Breakfast and dinner"
                )))
                .excludedItems(new ArrayList<>(Arrays.asList(
                        "Lunch and Drinks",
                        "Tips and Gratuities",
                        "Any Private Expenses"
                )))
                .itinerary(new ArrayList<>(Arrays.asList(
                        "Day 1-10: Comprehensive tour through Morocco's highlights"
                )))
                .availableDates(new ArrayList<>(Arrays.asList(
                        LocalDate.now().plusDays(5),
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(15)
                )))
                .destination(saharaDesert)
                .tourType(Activity.TourType.SHARED)
                .build();
        
        Activity relatedActivity2 = Activity.builder()
                .title("10 Days Authentic Morocco Journey from Casablanca")
                .slug(SlugUtil.generateSlug("10 Days Authentic Morocco Journey from Casablanca"))
                .shortDescription("Authentic 10-day journey through Morocco starting from Casablanca")
                .fullDescription("An authentic 10-day journey through Morocco starting from Casablanca, experiencing the real culture, traditions, and beauty of Morocco.")
                .price(new BigDecimal("99.00"))
                .duration("10 Days")
                .location("Casablanca to Morocco")
                .category("Shared Sahara Desert Tours")
                .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                .ratingAverage(new BigDecimal("4.8"))
                .reviewCount(38)
                .featured(true)
                .active(true)
                .maxGroupSize(16)
                .availableSlots(32)
                .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                .galleryImages(new ArrayList<>(Arrays.asList(
                        "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                        "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200"
                )))
                .availability("Everyday")
                .departureLocation("Casablanca")
                .returnLocation("Casablanca")
                .meetingTime("8 AM at your accommodation")
                .whatToExpect("An authentic journey through Morocco experiencing real culture, traditions, and beauty.")
                .includedItems(new ArrayList<>(Arrays.asList(
                        "Transportation in shared vehicle",
                        "Professional driver/guide",
                        "Hotel accommodation",
                        "Breakfast and dinner"
                )))
                .excludedItems(new ArrayList<>(Arrays.asList(
                        "Lunch and Drinks",
                        "Tips and Gratuities",
                        "Any Private Expenses"
                )))
                .itinerary(new ArrayList<>(Arrays.asList(
                        "Day 1-10: Authentic journey through Morocco's culture and traditions"
                )))
                .availableDates(new ArrayList<>(Arrays.asList(
                        LocalDate.now().plusDays(5),
                        LocalDate.now().plusDays(10),
                        LocalDate.now().plusDays(15)
                )))
                .destination(saharaDesert)
                .tourType(Activity.TourType.SHARED)
                .build();
        
        activityRepository.saveAll(Arrays.asList(relatedActivity1, relatedActivity2));
        System.out.println("Created 2 additional Sahara Desert activities");
    }

    /**
     * Grand Morocco circuit aligned with the itinerary described on Roaming Camels Morocco
     * (12 Days in Morocco — including Chefchaouen). Pinned first in listings via {@link Activity#getDisplayOrder()}.
     */
    private void seed12DaysInMoroccoIncludingChefchaouenIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream()
                .filter(d -> "Sahara Desert".equals(d.getName()))
                .findFirst()
                .orElse(null);
        if (saharaDesert == null) {
            System.out.println("Skipping 12-day Morocco tour seed: Sahara Desert destination missing.");
            return;
        }

        final String slug = "12-days-in-morocco-including-chefchaouen";
        final String title = "12 Days in Morocco (Including Chefchaouen)";
        final String shortDescription =
                "Casablanca · Chefchaouen · Fes · Sahara Desert · Todgha Gorge · Ait Ben Haddou · Essaouira · Marrakech — "
                        + "an extended private circuit with daily breakfast and key dinners in the Sahara, Todgha Gorge, and Ait Ben Haddou.";
        final String fullDescription = """
                Morocco travel is one of our favorite past times. Come along with us on this magical discovery of all that Morocco has to offer.

                Start your trip in Casablanca and visit Chefchaouen, the famous Blue City nestled into the Rif Mountains in the north before exploring the famous medina of Fes. Then it is on to the Sahara Desert and your dream come true on the back of a camel. Tour Morocco’s southern region and head to Marrakech where you will end the trip with the sights, sounds, and smell of exotic spices and snail soup in Jemaa el-Fnaa.""";

        final String whatToExpect = """
                Extended private tour — starts from Casablanca Airport. 12 days on the road with a comfortable air-conditioned vehicle, English-speaking driver (national guide when the group is over 4 people), and local city guides as needed.

                Meals: daily breakfast; dinners included in the Sahara, at Todgha Gorge, and at Ait Ben Haddou Kasbah (as per itinerary). Lunches, beverages, and most other dinners are on your own unless noted.

                Highlights include Rabat and the road north to Chefchaouen, two days in Fes with a full medina day, camel trek and desert camp in Merzouga, Todgha Gorge, Dades and Roses Valley, Ait Ben Haddou, the Atlantic port town of Essaouira, and two days in Marrakech before returning to Casablanca for departure.

                Optional add-ons you can request: cooking class, Marrakech food tour, hammam bookings, specialized guides, and extra touring on request.""";

        List<String> itinerary = new ArrayList<>(Arrays.asList(
                "Day 1: Casablanca — Arrive in Casablanca. Rest after your flight and visit the Hassan II Mosque (note: mosque interior visits have specific visitor rules and may not mirror every public tour page).",
                "Day 2: Chefchaouen — Drive north via Rabat for a highlights visit (Hassan Tower & Mausoleum area, Kasbah of the Oudayas and its gardens). Continue into the Rif Mountains to Chefchaouen.",
                "Day 3: Fes — Morning in Chefchaouen’s medina, then travel to Fes and settle into the ancient imperial city.",
                "Day 4: Fes — Full day with a local guide: Royal Palace gates, Mellah, medina lanes, artisan workshops (weavers, ceramics, brass), and the famous tanneries (UNESCO-related heritage). Lunch along the way.",
                "Day 5: Sahara Desert — Cross the Middle Atlas through Ifrane and cedar forests (Barbary macaques), via Midelt toward the Ziz Valley and Merzouga. Camel trek to camp; dinner under the stars (4x4 transfer optional instead of camels).",
                "Day 6: Todgha Gorge — Return from the dunes, travel via Erfoud and the Valley of Kasbahs toward Todgha Gorge; time along the dramatic canyon walls. Overnight in the valley (e.g. traditional guesthouse style).",
                "Day 7: Ait Ben Haddou Kasbah — Dades Gorge, Roses Valley, Ouarzazate, and the ksar of Ait Ben Haddou with a walk through the village. Dinner included at the kasbah stay.",
                "Day 8: Essaouira — Head west to Essaouira: medina of artists and musicians, working fishing port, long beach walks, and sunset over the Atlantic.",
                "Day 9: Marrakech — Drive to Marrakech; optional argan cooperative stop. Evening in Jemaa el-Fnaa — juice stalls, food stalls, and the square’s nightly theatre.",
                "Day 10: Marrakech — Guided half-day: Koutoubia surrounds, Bahia Palace, Saadian Tombs, Mellah; free time for souks, Majorelle Garden, or a hammam.",
                "Day 11: Casablanca — Flexible transfer to Casablanca — leave early or add last Marrakech sights with your driver.",
                "Day 12: Departure — Depart from Casablanca (airport transfer as arranged)."
        ));

        List<String> included = new ArrayList<>(Arrays.asList(
                "Airport transfers",
                "Local guides as needed",
                "Daily breakfast",
                "Some dinners (Sahara camp, Todgha Gorge area, Ait Ben Haddou per itinerary)",
                "Admission to historic monuments as listed in the itinerary (exceptions: Jardin Majorelle and Hassan II Mosque may require direct tickets)",
                "Comfortable air-conditioned vehicle",
                "All accommodations",
                "Camel trek in the desert",
                "English-speaking driver or national guide (over 4 people)"
        ));

        List<String> excluded = new ArrayList<>(Arrays.asList(
                "Airfare",
                "Lunch",
                "Discretionary gratuities (driver, guides, servers)",
                "Travel insurance",
                "Dinner unless otherwise indicated",
                "Beverages"
        ));

        List<String> complementaries = new ArrayList<>(Arrays.asList(
                "Cooking class (on request)",
                "Food tour — Marrakech (on request)",
                "Additional tours on request",
                "Specialized guides (on request)",
                "Hammam bookings (on request)"
        ));

        Optional<Activity> existing = activityRepository.findBySlug(slug);
        Activity activity;
        if (existing.isPresent()) {
            activity = existing.get();
        } else {
            activity = Activity.builder().slug(slug).build();
        }

        activity.setTitle(title);
        activity.setShortDescription(shortDescription);
        activity.setFullDescription(fullDescription);
        activity.setWhatToExpect(whatToExpect);
        activity.setPrice(new BigDecimal("2499.00"));
        activity.setPremiumPrice(new BigDecimal("3199.00"));
        activity.setBudgetPrice(new BigDecimal("2199.00"));
        activity.setDuration("12 Days");
        activity.setLocation("Casablanca - Chefchaouen - Essaouira - Fes - Marrakech - Sahara Desert");
        activity.setCategory("Extended Tour");
        activity.setDifficultyLevel(Activity.DifficultyLevel.MODERATE);
        activity.setTourType(Activity.TourType.PRIVATE);
        activity.setRatingAverage(new BigDecimal("4.9"));
        activity.setReviewCount(24);
        activity.setFeatured(true);
        activity.setDisplayOrder(0);
        activity.setActive(true);
        activity.setMaxGroupSize(12);
        activity.setAvailableSlots(24);
        activity.setImageUrl("https://images.unsplash.com/photo-1569383746724-4f2c9b14e68b?w=1200");
        activity.setGalleryImages(new ArrayList<>(Arrays.asList(
                "https://images.unsplash.com/photo-1569383746724-4f2c9b14e68b?w=1200",
                "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                "https://images.unsplash.com/photo-1489749798305-4fea3ae63d43?w=1200",
                "https://images.unsplash.com/photo-1539025638867-da25744e7a1e?w=1200"
        )));
        activity.setAvailability("On request — private departures");
        activity.setDepartureLocation("Casablanca Airport");
        activity.setReturnLocation("Casablanca Airport");
        activity.setMeetingTime("Meet your driver at Casablanca Airport or your Casablanca hotel — time confirmed before travel");
        activity.setIncludedItems(included);
        activity.setExcludedItems(excluded);
        activity.setComplementaries(complementaries);
        activity.setItinerary(itinerary);
        activity.setDestination(saharaDesert);
        activity.setMapUrl("https://www.google.com/maps/search/Morocco");
        activity.setAvailableDates(new ArrayList<>(Arrays.asList(
                LocalDate.now().plusDays(14),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(45)
        )));

        activityRepository.save(activity);
        System.out.println("Seeded / updated activity: " + title + " (" + slug + ")");
    }

    /**
     * Extended one-week circuit aligned with Roaming Camels Morocco:
     * https://roamingcamelsmorocco.com/tours/7-days-tour-casablanca-to-marrakech/
     */
    private void seed7DaysTourCasablancaToMarrakechIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream()
                .filter(d -> "Sahara Desert".equals(d.getName()))
                .findFirst()
                .orElse(null);
        if (saharaDesert == null) {
            System.out.println("Skipping 7-day Casablanca→Marrakech tour seed: Sahara Desert destination missing.");
            return;
        }

        final String slug = "7-days-tour-casablanca-to-marrakech";
        final String title = "7 Days Tour- Casablanca to Marrakech";
        final String shortDescription =
                "Casablanca · Rabat · Meknes · Volubilis · Fes · Sahara Desert · Dades Valley · Ait Ben Haddou · Marrakech — "
                        + "a one-week private tour with daily breakfast and (2) dinners in the Sahara and Dades Valley.";
        final String fullDescription = """
                Do you have 7 days and 6 nights to spare? This one-week Morocco tour covers the highlights efficiently — cities, countryside, desert landscapes, and oasis stops — with door-to-door service from arrival to departure.

                Visit the key city sights in Casablanca and Rabat, explore Meknes and the Roman ruins at Volubilis, then enjoy a private guided day in Fes’ ancient medina. Continue through the Middle Atlas toward the Sahara for a camel trek and desert camp experience, then travel via the valleys and kasbahs (including Ait Ben Haddou) before ending in vibrant Marrakech.""";

        final String whatToExpect = """
                Starts at Casablanca Airport. Private car and driver, accommodations pre-booked with your approval, and local guides arranged as needed.

                Meals: daily breakfast; two dinners included (Sahara & Dades Valley). Lunches, beverages, and other dinners are on your own unless noted.

                Highlights include Casablanca & Rabat, Meknes and Volubilis, a private Fes medina tour, Middle Atlas landscapes (Ifrane, cedar forests), Sahara dunes with camel trek and camp, Dades Valley, Ait Ben Haddou, and Marrakech (including an evening in Jemaa el-Fnaa).""";

        List<String> itinerary = new ArrayList<>(Arrays.asList(
                "Day 1: Arrive in Casablanca — Airport pick-up, transfer to your accommodation. Visit Hassan II Mosque exterior (or interior tour, time permitting).",
                "Day 2: Rabat → Fes — Drive from Casablanca to Rabat for a city tour (Hassan Tower, Chellah, medina, Kasbah of the Oudayas, and nearby Sale). Continue toward Fes.",
                "Day 3: Fes — Private city tour with a local guide through the old medina’s narrow alleys, artisan quarters, and historic sites.",
                "Day 4: Sahara Desert — Stop in Ifrane, cross the Middle Atlas and cedar forests (Barbary macaques), lunch in Midelt, scenic drive along Ziz Valley, then reach the dunes for your desert night.",
                "Day 5: Dades Valley — Sunrise on the dunes, camel trek back for breakfast and shower, then continue south toward the valleys and gorges en route to Dades Valley.",
                "Day 6: Ait Ben Haddou → Marrakech — Explore Ait Ben Haddou, cross the High Atlas via Tizi n’ Tichka, arrive in Marrakech and enjoy an evening in Jemaa el-Fnaa.",
                "Day 7: Departure — Depart from Marrakech, or transfer back to Casablanca (CMN) if your flight requires it."
        ));

        List<String> included = new ArrayList<>(Arrays.asList(
                "Airport transfers",
                "Local guides as needed",
                "Daily breakfast",
                "Some dinners (2: Sahara & Dades Valley)",
                "Admission to historic monuments as listed (exceptions: Jardin Majorelle and Hassan II Mosque)",
                "Comfortable air-conditioned vehicle",
                "All accommodations",
                "Camel trek in the desert",
                "English-speaking driver or national guide (over 4 people)"
        ));

        List<String> excluded = new ArrayList<>(Arrays.asList(
                "Airfare",
                "Lunch",
                "Discretionary gratuities (driver/guide, city guides, servers)",
                "Travel insurance",
                "Dinner unless otherwise indicated",
                "Beverages"
        ));

        List<String> complementaries = new ArrayList<>(Arrays.asList(
                "Cooking class (on request)",
                "Food tour (Marrakech)",
                "Additional tours on request",
                "Specialized guides",
                "Hammam bookings"
        ));

        Optional<Activity> existing = activityRepository.findBySlug(slug);
        Activity activity = existing.orElseGet(() -> Activity.builder().slug(slug).build());

        activity.setTitle(title);
        activity.setShortDescription(shortDescription);
        activity.setFullDescription(fullDescription);
        activity.setWhatToExpect(whatToExpect);
        activity.setPrice(new BigDecimal("1599.00"));
        activity.setPremiumPrice(new BigDecimal("2099.00"));
        activity.setBudgetPrice(new BigDecimal("1399.00"));
        activity.setDuration("7 Days");
        activity.setLocation("Casablanca-Fes-Marrakech-Sahara Desert");
        activity.setCategory("Extended Tour");
        activity.setDifficultyLevel(Activity.DifficultyLevel.MODERATE);
        activity.setTourType(Activity.TourType.PRIVATE);
        activity.setRatingAverage(new BigDecimal("4.9"));
        activity.setReviewCount(18);
        activity.setFeatured(true);
        activity.setDisplayOrder(1);
        activity.setActive(true);
        activity.setMaxGroupSize(12);
        activity.setAvailableSlots(24);
        activity.setImageUrl("https://images.unsplash.com/photo-1489749798305-4fea3ae63d43?w=1200");
        activity.setGalleryImages(new ArrayList<>(Arrays.asList(
                "https://images.unsplash.com/photo-1489749798305-4fea3ae63d43?w=1200",
                "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                "https://images.unsplash.com/photo-1569383746724-4f2c9b14e68b?w=1200"
        )));
        activity.setAvailability("On request — private departures");
        activity.setDepartureLocation("Casablanca Airport");
        activity.setReturnLocation("Marrakech (or Casablanca Airport)");
        activity.setMeetingTime("Meet your driver at Casablanca Airport or your Casablanca hotel — time confirmed before travel");
        activity.setIncludedItems(included);
        activity.setExcludedItems(excluded);
        activity.setComplementaries(complementaries);
        activity.setItinerary(itinerary);
        activity.setDestination(saharaDesert);
        activity.setMapUrl("https://www.google.com/maps/search/Casablanca+to+Marrakech+Morocco");
        activity.setAvailableDates(new ArrayList<>(Arrays.asList(
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(21),
                LocalDate.now().plusDays(35)
        )));

        activityRepository.save(activity);
        System.out.println("Seeded / updated activity: " + title + " (" + slug + ")");
    }
    
    private void seed4DaysTourIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            return;
        }
        
        // Always call seed4DaysTour to ensure activity and translations are up to date
        seed4DaysTour(saharaDesert);
    }
    
    private void seed4DaysTour(Destination saharaDesert) {
        String slug = SlugUtil.generateSlug("Tour from Fes to Marrakech 4 Days");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        boolean isNew = false;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - preserve all user modifications
            activity = existingActivity.get();
            // Only update critical fields when opt-in flags are true (default: preserve admin destination/active)
            boolean needsSave = false;
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            if (needsSave) {
                activityRepository.save(activity);
            }
            // Skip creating new activity - preserve existing
            return;
        } else {
            isNew = true;
            activity = Activity.builder()
                    .title("Tour from Fes to Marrakech 4 Days")
                    .slug(slug)
                    .shortDescription("Do you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty.")
                    .fullDescription("Do you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty. You will fully enjoy the pleasure of roaming freely on some of the most beautiful slopes and enjoy the beautiful sunset in the Sahara. A short but full impression of this area of Morocco with its Mountains, gorges, valleys, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the sunrise, and the hospitality of the people.\n\nDuring this 4 Days Tour from Fes to Marrakech, you will explore the imperial cities, high atlas mountains, southern market town renowned Unesco Heritage site, or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Sahara Desert Tours to the Merzouga and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .price(new BigDecimal("180.00"))
                    .duration("4 Days")
                    .location("From Fes to Marrakech")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.8"))
                    .reviewCount(68)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(32)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Fes")
                    .returnLocation("Marrakech")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("A Tour from Fes to Marrakech is an unforgettable experience that will allow you to go through the history and the hidden culture and traditions of the daily life of Morocco as well as the discovery of the old imperial cities in Morocco.\n\nDo you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty. You will fully enjoy the pleasure of roaming freely on some of the most beautiful slopes and enjoy the beautiful sunset in the Sahara. A short but full impression of this area of Morocco with its Mountains, gorges, green palm groves, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the camel ride experience, and the hospitality of the people.\n\nDuring this 4 Days Tour from Fes to Marrakech, you will explore the imperial cities, high atlas mountains, southern market town renowned Unesco Heritage site, or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Sahara Desert Tours to the Merzouga and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: Fes - Azrou - Ziz Valley - Sahara Desert. Your driver/guide will pick you up at your Fes accommodation around 8 am for a beautiful journey to the beautiful dunes of Merzouga, passing by Ifran known as \"the Switzerland Moroccan\", one hour drive, we will stop to visit the cedar forest in Azrou and see the monkeys climbing cedar trees. Then follow the path and cross the mountains of the Middle Atlas, passing by Midelt, stop at Ziz valley for pictures of stunning panoramic views over the gorges of the Ziz River, next to the city Erfoud of famous by fossil where are extracted the rocks containing fossils, Arrival at Merzouga dunes \"Erg Chebbi\" where we can feel the peace and tranquility of the desert. Relax and sip a mint tea enjoying the silence of the Sahara. Dinner and overnight at a hotel or Riad located at the foot of the dunes.",
                            "Day 2: Sahara Desert Tour and Visit to nomads. After breakfast at your hotel, departure for a visit to the village (Khamlia) where live Gnawa of African origin, settle in the desert of Morocco in search of a better life, followed by a visit to a nomad family to learn about the traditions and lifestyle, afterward, we will head to the city of Rissani exploring its souk where you can get yourself a bath of local products and have lunch in a restaurant to taste the Berber pizza (Medfouna). In the afternoon after relaxing in your hotel, we will then, get ready for a new adventure experience, to a Berber camp in a lifetime experience, you will ride a camel through the golden dunes of Erg Chebbi, to assist to a wonderful sunset and spend a romantic night in the desert camp. After dinner, we gather around the fire and enjoy the desert night, perhaps with traditional Berber drums.",
                            "Day 3: Sahara Desert - Todra Gorges - Dades Valley - Ouarzazate. Early wake up for a lifetime sunrise over the dunes, you will enjoy the change of sand color with the light of the sun. Prepare your cameras and your phones to take some memorable pictures. After taking photos, you will take shower and breakfast, and then, you will ride camels back to the hotel enjoying the silence and the charm of the dunes with the sunlight. Our Tour from Fes to Marrakech continues for a new journey through the palm groves and oasis direction to Tinghir and its amazing Todra Gorges and the big canyons 300 m high. After the visit, we will continue to Dades gorges above the valley. Many stops for photos along the valley, Then continue to Kalaat Magouna (Rose Valley) famous for its annual festival of roses that takes place every year in May. After visiting a female cooperative of roses, we will drive to Ouarzazate through the oasis of Skoura, where we will stop to visit the old Kasbah Amrideil (500 years old) then arrive in Ouarzazate. Dinner and overnight at the hotel.",
                            "Day 4: Ouarzazate - Kasbah Ait Benhaddou - High Atlas - Marrakech. Breakfast at your Hotel or Riad in Ouarzazate, then, we will start a new journey of our Tour from Fes to Marrakech, our first stop for today is to visit the city's famous kasbah, including that of Taourirt marked as world heritage by Unesco and one of the beautiful palaces in the area. Not far from the city Ouarzazate Kasbah we will stop to visit the museum of movies and cinema studios, where many international movies are shot, like: (Gladiator, Lawrence d'Arabie, Games of the throne, the Babel…). 20 km later we will turn right to visit the famous kasbah of Ait Ben Haddou, after the visit we will continue crossing the high Atlas Mountains and its dramatic landscapes and the hidden Berber villages settled in the foothills of the massive valleys. (optional: we can have lunch in a local restaurant in the Tichka pass) else, we will continue to Marrakech if you want to use your left time visiting the amazing Majorelle gardens."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(21),
                            LocalDate.now().plusDays(28),
                            LocalDate.now().plusDays(35)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.PRIVATE)
                    .build();
        }
        
        activityRepository.save(activity);
        
        // Create or update translations for the 4-day tour
        seed4DaysTourTranslations(activity);
    }
    
    private void seed4DaysTourTranslations(Activity activity) {
        // Check if translations already exist and update them, otherwise create new ones
        List<ActivityTranslation> translations = new ArrayList<>();
        
        // French translation
        Optional<ActivityTranslation> existingFr = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "fr");
        ActivityTranslation frTranslation;
        if (existingFr.isPresent()) {
            frTranslation = existingFr.get();
            frTranslation.setTitle("Circuit de 4 jours de Fès à Marrakech");
            frTranslation.setShortDescription("Voulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle.");
            frTranslation.setFullDescription("Voulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle. Vous profiterez pleinement du plaisir de vous promener librement sur certaines des plus belles pentes et de profiter du magnifique coucher de soleil dans le Sahara. Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.\n\nAu cours de ce circuit de 4 jours de Fès à Marrakech, vous explorerez les villes impériales, les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.");
            frTranslation.setLocation("De Fès à Marrakech");
            frTranslation.setCategory("Circuits du désert du Sahara");
            frTranslation.setDepartureLocation("Fès");
            frTranslation.setReturnLocation("Marrakech");
            frTranslation.setMeetingTime("15 minutes avant l'heure de départ à 8h");
            frTranslation.setAvailability("Tous les jours");
            frTranslation.setWhatToExpect("Un circuit de Fès à Marrakech est une expérience inoubliable qui vous permettra de découvrir l'histoire et la culture cachée et les traditions de la vie quotidienne du Maroc ainsi que la découverte des anciennes villes impériales du Maroc.\n\nVoulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle. Vous profiterez pleinement du plaisir de vous promener librement sur certaines des plus belles pentes et de profiter du magnifique coucher de soleil dans le Sahara. Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, palmeraies vertes, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, l'expérience de la balade à dos de chameau et l'hospitalité des gens.");
        } else {
            frTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("fr")
                    .title("Circuit de 4 jours de Fès à Marrakech")
                    .shortDescription("Voulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle.")
                    .fullDescription("Voulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle. Vous profiterez pleinement du plaisir de vous promener librement sur certaines des plus belles pentes et de profiter du magnifique coucher de soleil dans le Sahara. Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.\n\nAu cours de ce circuit de 4 jours de Fès à Marrakech, vous explorerez les villes impériales, les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.")
                    .location("De Fès à Marrakech")
                    .category("Circuits du désert du Sahara")
                    .departureLocation("Fès")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutes avant l'heure de départ à 8h")
                    .availability("Tous les jours")
                    .whatToExpect("Un circuit de Fès à Marrakech est une expérience inoubliable qui vous permettra de découvrir l'histoire et la culture cachée et les traditions de la vie quotidienne du Maroc ainsi que la découverte des anciennes villes impériales du Maroc.\n\nVoulez-vous un vrai changement de décor loin de toutes les contraintes du monde moderne ? Ce voyage d'aventure dans le désert du Maroc vous mène au cœur du désert du Sahara dans des paysages d'une beauté à couper le souffle. Vous profiterez pleinement du plaisir de vous promener librement sur certaines des plus belles pentes et de profiter du magnifique coucher de soleil dans le Sahara. Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, palmeraies vertes, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, l'expérience de la balade à dos de chameau et l'hospitalité des gens.")
                    .build();
        }
        translations.add(frTranslation);
        
        // Spanish translation
        Optional<ActivityTranslation> existingEs = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "es");
        ActivityTranslation esTranslation;
        if (existingEs.isPresent()) {
            esTranslation = existingEs.get();
            esTranslation.setTitle("Tour de 4 días de Fez a Marrakech");
            esTranslation.setShortDescription("¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza.");
            esTranslation.setFullDescription("¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza. Disfrutarás plenamente del placer de deambular libremente por algunas de las pendientes más hermosas y disfrutar de la hermosa puesta de sol en el Sahara. Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.\n\nDurante este Tour de 4 días de Fez a Marrakech, explorarás las ciudades imperiales, las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables Tours del Desierto del Sahara a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.");
            esTranslation.setLocation("De Fez a Marrakech");
            esTranslation.setCategory("Tours del Desierto del Sahara");
            esTranslation.setDepartureLocation("Fez");
            esTranslation.setReturnLocation("Marrakech");
            esTranslation.setMeetingTime("15 minutos antes de la hora de salida a las 8 am");
            esTranslation.setAvailability("Todos los días");
            esTranslation.setWhatToExpect("Un tour de Fez a Marrakech es una experiencia inolvidable que te permitirá conocer la historia y la cultura oculta y las tradiciones de la vida diaria de Marruecos, así como el descubrimiento de las antiguas ciudades imperiales de Marruecos.\n\n¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza. Disfrutarás plenamente del placer de deambular libremente por algunas de las pendientes más hermosas y disfrutar de la hermosa puesta de sol en el Sahara. Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, palmeras verdes, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, la experiencia del paseo en camello y la hospitalidad de la gente.");
        } else {
            esTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("es")
                    .title("Tour de 4 días de Fez a Marrakech")
                    .shortDescription("¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza.")
                    .fullDescription("¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza. Disfrutarás plenamente del placer de deambular libremente por algunas de las pendientes más hermosas y disfrutar de la hermosa puesta de sol en el Sahara. Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.\n\nDurante este Tour de 4 días de Fez a Marrakech, explorarás las ciudades imperiales, las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables Tours del Desierto del Sahara a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.")
                    .location("De Fez a Marrakech")
                    .category("Tours del Desierto del Sahara")
                    .departureLocation("Fez")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutos antes de la hora de salida a las 8 am")
                    .availability("Todos los días")
                    .whatToExpect("Un tour de Fez a Marrakech es una experiencia inolvidable que te permitirá conocer la historia y la cultura oculta y las tradiciones de la vida diaria de Marruecos, así como el descubrimiento de las antiguas ciudades imperiales de Marruecos.\n\n¿Quieres un verdadero cambio de escenario lejos de todas las limitaciones del mundo moderno? Este viaje de aventura en el desierto de Marruecos te lleva al corazón del desierto del Sahara en paisajes de belleza. Disfrutarás plenamente del placer de deambular libremente por algunas de las pendientes más hermosas y disfrutar de la hermosa puesta de sol en el Sahara. Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, palmeras verdes, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, la experiencia del paseo en camello y la hospitalidad de la gente.")
                    .build();
        }
        translations.add(esTranslation);
        
        // German translation
        Optional<ActivityTranslation> existingDe = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "de");
        ActivityTranslation deTranslation;
        if (existingDe.isPresent()) {
            deTranslation = existingDe.get();
            deTranslation.setTitle("4-Tage-Tour von Fes nach Marrakesch");
            deTranslation.setShortDescription("Möchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit.");
            deTranslation.setFullDescription("Möchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit. Sie werden das Vergnügen genießen, frei auf einigen der schönsten Hänge zu wandern und den wunderschönen Sonnenuntergang in der Sahara zu genießen. Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.\n\nWährend dieser 4-Tage-Tour von Fes nach Marrakesch erkunden Sie die kaiserlichen Städte, das Hohe Atlasgebirge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.");
            deTranslation.setLocation("Von Fes nach Marrakesch");
            deTranslation.setCategory("Sahara-Wüstentouren");
            deTranslation.setDepartureLocation("Fes");
            deTranslation.setReturnLocation("Marrakesch");
            deTranslation.setMeetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr");
            deTranslation.setAvailability("Täglich");
            deTranslation.setWhatToExpect("Eine Tour von Fes nach Marrakesch ist ein unvergessliches Erlebnis, das es Ihnen ermöglicht, durch die Geschichte und die verborgene Kultur und Traditionen des täglichen Lebens in Marokko zu gehen sowie die Entdeckung der alten kaiserlichen Städte in Marokko.\n\nMöchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit. Sie werden das Vergnügen genießen, frei auf einigen der schönsten Hänge zu wandern und den wunderschönen Sonnenuntergang in der Sahara zu genießen. Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, grünen Palmenoasen, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Kamelritt-Erlebnis und der Gastfreundschaft der Menschen.");
        } else {
            deTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("de")
                    .title("4-Tage-Tour von Fes nach Marrakesch")
                    .shortDescription("Möchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit.")
                    .fullDescription("Möchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit. Sie werden das Vergnügen genießen, frei auf einigen der schönsten Hänge zu wandern und den wunderschönen Sonnenuntergang in der Sahara zu genießen. Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.\n\nWährend dieser 4-Tage-Tour von Fes nach Marrakesch erkunden Sie die kaiserlichen Städte, das Hohe Atlasgebirge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.")
                    .location("Von Fes nach Marrakesch")
                    .category("Sahara-Wüstentouren")
                    .departureLocation("Fes")
                    .returnLocation("Marrakesch")
                    .meetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr")
                    .availability("Täglich")
                    .whatToExpect("Eine Tour von Fes nach Marrakesch ist ein unvergessliches Erlebnis, das es Ihnen ermöglicht, durch die Geschichte und die verborgene Kultur und Traditionen des täglichen Lebens in Marokko zu gehen sowie die Entdeckung der alten kaiserlichen Städte in Marokko.\n\nMöchten Sie eine echte Veränderung der Szenerie fernab aller Zwänge der modernen Welt? Diese Abenteuerreise in die Wüste Marokkos führt Sie ins Herz der Sahara in Landschaften von atemberaubender Schönheit. Sie werden das Vergnügen genießen, frei auf einigen der schönsten Hänge zu wandern und den wunderschönen Sonnenuntergang in der Sahara zu genießen. Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, grünen Palmenoasen, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Kamelritt-Erlebnis und der Gastfreundschaft der Menschen.")
                    .build();
        }
        translations.add(deTranslation);
        
        // Save all translations
        activityTranslationRepository.saveAll(translations);
    }
    
    private void seed4DaysDesertTripFromMarrakeshIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            return;
        }
        
        // Always call seed4DaysDesertTripFromMarrakesh to ensure activity and translations are up to date
        seed4DaysDesertTripFromMarrakesh(saharaDesert);
    }
    
    private void seed4DaysDesertTripFromMarrakesh(Destination saharaDesert) {
        String slug = SlugUtil.generateSlug("4 Days Desert Trip from Marrakesh");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        boolean isNew = false;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - preserve all user modifications
            activity = existingActivity.get();
            // Only update critical fields when opt-in flags are true (default: preserve admin destination/active)
            boolean needsSave = false;
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            if (needsSave) {
                activityRepository.save(activity);
            }
            // Skip creating new activity - preserve existing
            return;
        } else {
            isNew = true;
            activity = Activity.builder()
                    .title("4 Days Desert Trip from Marrakesh")
                    .slug(slug)
                    .shortDescription("A short but full impression of this area of Morocco with its Mountains, gorges, valleys, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the sunrise, and the hospitality of the people.")
                    .fullDescription("Marrakesh is home to some of the most extraordinary structures including the Kasbah, a number of brilliant mosques, an open-air theatre, palaces, and gardens that attract tourists from all over the world. From reasonable shopping in famous Medina souks to historical sightseeing of the many museums and monuments, this place has it all. The High Atlas region of Marakkech provides access to the picturesque mountain beauty.\n\nThis 4 Day Desert Trip from Marrakech to Fes will take you on a Tour from Marrakech 4 days to explore the high atlas mountains, southern market town renowned Unesco Heritage site, or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Sahara Desert Tours 4 days to the Merzouga and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .price(new BigDecimal("99.00"))
                    .duration("4 Days")
                    .location("Marrakech to Sahara Desert")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.8"))
                    .reviewCount(1709)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(32)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("Marrakech is home to some of the most extraordinary structures including the Kasbah, a number of brilliant mosques, an open-air theatre, palaces, and gardens that attract tourists from all over the world. From reasonable shopping in famous Medina souks to historical sightseeing of the many museums and monuments, this place has it all. The High Atlas region of Marakkech provides access to the picturesque mountain beauty.\n\nDuring this 4 Day Desert Trip from Marrakech, you get a chance to discover the imperial cities of Marrakech as well as the Sahara Desert, it's a mix of culture, history, and Unesco Heritage site. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for a memorable trip to the Sahara desert and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: MARRAKECH - AIT BEN HADDOU KASBAH - OUARZAZATE - ROSES VALLEY - DADES GORGES. Your driver/guide will pick you up at your Marrakech accommodation at 8 am and drive through the route passes through valleys and deserts allowing you to enjoy spectacular views from the Atlas Mountains. We will go through the Tizi n'Tichka, a pass at an altitude of 2,260 meters: the highest pass in North Africa and it has become famous for the breathtaking view it offers. Halfway, you will benefit from a free visit to the famous Kasbah Ait Ben Haddou, a fortified city classified as a UNESCO World Heritage Site. Lunch in a local restaurant overlooking the kasbah, and then we will continue our journey towards Ouarzazate known as the gate of the desert, where we will stop at the cinema studios for a short visit (optional), and then, we will continue to the heart of the city to discover another hidden gem, the kasbah Taourirte a world heritage site by UNESCO and the house of the leader Pacha el glamour, after a visit of the kasbah we will continue our journey towards our final destination for today the Dades gorges, on the road we will stop at the rises valley for a short visit to a local cooperative producing the rose water and its products. After a short drive, we will arrive at the Dades gorges where we will spend the night in a local hotel or Riad",
                            "Day 2: DADES GORGES - TODRA GORGES - ERFOUD - MERZOUGA DESERT. After breakfast at your hotel, we will start our day with a visit to the Dades Gorges, then, we will head towards Todra Gorges, the favorite place for rock climbers. After a visit to the gorges and the palm oasis of Todra, we continue our journey, following the ancient Caravan trade routes to the Sahara passing through a series of fortified villages with outstanding pre-Saharan architecture. The route to Erfoud is one of the most pleasant of all the southern routes. The city is in a dry red built of the desert and was built by the French as a central administration city. It is known for its rich black marble fossils. After Erfoud, we will continue our day heading towards the spectacular dunes of Merzouga. At our arrival, we will be welcomed by a mint tea, then, get ready for a new adventure experience, to a Berber camp in a lifetime experience, you will ride a camel through the golden dunes of Erg Chebbi, to assist to a wonderful sunset and spend a romantic night in desert camp. After dinner, we gather around the fire and enjoy the desert night, perhaps with traditional Berber drums.",
                            "Day 3: MERZOUGA - RISSANI - ANTI ATLAS - DRAA VALLEY - ZAGORA. Wake up early before sunrise to enjoy the sunrise from the Great Dune of the Sahara Desert. Return to the hotel in dromedaries or in 4WD to meet your driver to start a new journey to Rissani and then to Alnif and Tazzarine until N'Kob; stop for lunch. The trip continues through the palm-lined Draa Valley until we arrive at Zagora, a very important point on the old caravan route and very famous for the date's production. Dinner and overnight in a charming hotel.",
                            "Day 4: ZAGORA - OUARZAZATE - MARRAKECH. After breakfast, you will have enough time to visit Tamegroute and its old Koran library and subterranean Kasbah. After the visit, we will return through the Draa- river valley to Quarzazate (stop for lunch). The journey will end in Marrakech with a scenic drive via the High Atlas Mountains."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(17),
                            LocalDate.now().plusDays(21),
                            LocalDate.now().plusDays(24),
                            LocalDate.now().plusDays(28),
                            LocalDate.now().plusDays(31),
                            LocalDate.now().plusDays(35),
                            LocalDate.now().plusDays(42)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.SHARED)
                    .build();
        }
        
        activityRepository.save(activity);
        
        // Create or update translations for the 4-day desert trip from Marrakesh
        seed4DaysDesertTripFromMarrakeshTranslations(activity);
    }
    
    private void seed4DaysDesertTripFromMarrakeshTranslations(Activity activity) {
        // Check if translations already exist and update them, otherwise create new ones
        List<ActivityTranslation> translations = new ArrayList<>();
        
        // French translation
        Optional<ActivityTranslation> existingFr = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "fr");
        ActivityTranslation frTranslation;
        if (existingFr.isPresent()) {
            frTranslation = existingFr.get();
            frTranslation.setTitle("Circuit de 4 jours dans le désert depuis Marrakech");
            frTranslation.setShortDescription("Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.");
            frTranslation.setFullDescription("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nCe circuit de 4 jours dans le désert depuis Marrakech jusqu'à Fès vous emmènera dans un circuit de 4 jours depuis Marrakech pour explorer les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara de 4 jours à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.");
            frTranslation.setLocation("Marrakech vers le désert du Sahara");
            frTranslation.setCategory("Circuits du désert du Sahara");
            frTranslation.setDepartureLocation("Marrakech");
            frTranslation.setReturnLocation("Marrakech");
            frTranslation.setMeetingTime("15 minutes avant l'heure de départ à 8h");
            frTranslation.setAvailability("Tous les jours");
            frTranslation.setWhatToExpect("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nAu cours de ce circuit de 4 jours dans le désert depuis Marrakech, vous aurez l'occasion de découvrir les villes impériales de Marrakech ainsi que le désert du Sahara, c'est un mélange de culture, d'histoire et de site du patrimoine de l'Unesco. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour un voyage mémorable dans le désert du Sahara et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.");
        } else {
            frTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("fr")
                    .title("Circuit de 4 jours dans le désert depuis Marrakech")
                    .shortDescription("Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.")
                    .fullDescription("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nCe circuit de 4 jours dans le désert depuis Marrakech jusqu'à Fès vous emmènera dans un circuit de 4 jours depuis Marrakech pour explorer les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara de 4 jours à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.")
                    .location("Marrakech vers le désert du Sahara")
                    .category("Circuits du désert du Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutes avant l'heure de départ à 8h")
                    .availability("Tous les jours")
                    .whatToExpect("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nAu cours de ce circuit de 4 jours dans le désert depuis Marrakech, vous aurez l'occasion de découvrir les villes impériales de Marrakech ainsi que le désert du Sahara, c'est un mélange de culture, d'histoire et de site du patrimoine de l'Unesco. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour un voyage mémorable dans le désert du Sahara et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.")
                    .build();
        }
        translations.add(frTranslation);
        
        // Spanish translation
        Optional<ActivityTranslation> existingEs = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "es");
        ActivityTranslation esTranslation;
        if (existingEs.isPresent()) {
            esTranslation = existingEs.get();
            esTranslation.setTitle("Viaje de 4 días al desierto desde Marrakech");
            esTranslation.setShortDescription("Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.");
            esTranslation.setFullDescription("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nEste viaje de 4 días al desierto desde Marrakech hasta Fez te llevará en un tour de 4 días desde Marrakech para explorar las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables tours del desierto del Sahara de 4 días a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.");
            esTranslation.setLocation("Marrakech al desierto del Sahara");
            esTranslation.setCategory("Tours del Desierto del Sahara");
            esTranslation.setDepartureLocation("Marrakech");
            esTranslation.setReturnLocation("Marrakech");
            esTranslation.setMeetingTime("15 minutos antes de la hora de salida a las 8 am");
            esTranslation.setAvailability("Todos los días");
            esTranslation.setWhatToExpect("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nDurante este viaje de 4 días al desierto desde Marrakech, tendrás la oportunidad de descubrir las ciudades imperiales de Marrakech así como el desierto del Sahara, es una mezcla de cultura, historia y sitio del Patrimonio de la Unesco. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para un viaje memorable al desierto del Sahara y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.");
        } else {
            esTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("es")
                    .title("Viaje de 4 días al desierto desde Marrakech")
                    .shortDescription("Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.")
                    .fullDescription("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nEste viaje de 4 días al desierto desde Marrakech hasta Fez te llevará en un tour de 4 días desde Marrakech para explorar las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables tours del desierto del Sahara de 4 días a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.")
                    .location("Marrakech al desierto del Sahara")
                    .category("Tours del Desierto del Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutos antes de la hora de salida a las 8 am")
                    .availability("Todos los días")
                    .whatToExpect("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nDurante este viaje de 4 días al desierto desde Marrakech, tendrás la oportunidad de descubrir las ciudades imperiales de Marrakech así como el desierto del Sahara, es una mezcla de cultura, historia y sitio del Patrimonio de la Unesco. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para un viaje memorable al desierto del Sahara y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.")
                    .build();
        }
        translations.add(esTranslation);
        
        // German translation
        Optional<ActivityTranslation> existingDe = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "de");
        ActivityTranslation deTranslation;
        if (existingDe.isPresent()) {
            deTranslation = existingDe.get();
            deTranslation.setTitle("4-Tage-Wüstentour ab Marrakesch");
            deTranslation.setShortDescription("Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.");
            deTranslation.setFullDescription("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nDiese 4-Tage-Wüstentour von Marrakesch nach Fes führt Sie auf eine 4-Tage-Tour von Marrakesch, um die hohen Atlas-Berge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs zu erkunden. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen 4-Tage-Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.");
            deTranslation.setLocation("Marrakesch zur Sahara-Wüste");
            deTranslation.setCategory("Sahara-Wüstentouren");
            deTranslation.setDepartureLocation("Marrakesch");
            deTranslation.setReturnLocation("Marrakesch");
            deTranslation.setMeetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr");
            deTranslation.setAvailability("Täglich");
            deTranslation.setWhatToExpect("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nWährend dieser 4-Tage-Wüstentour von Marrakesch haben Sie die Gelegenheit, die kaiserlichen Städte von Marrakesch sowie die Sahara-Wüste zu entdecken, es ist eine Mischung aus Kultur, Geschichte und UNESCO-Weltkulturerbe. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für eine unvergessliche Reise in die Sahara-Wüste und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.");
        } else {
            deTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("de")
                    .title("4-Tage-Wüstentour ab Marrakesch")
                    .shortDescription("Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.")
                    .fullDescription("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nDiese 4-Tage-Wüstentour von Marrakesch nach Fes führt Sie auf eine 4-Tage-Tour von Marrakesch, um die hohen Atlas-Berge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs zu erkunden. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen 4-Tage-Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.")
                    .location("Marrakesch zur Sahara-Wüste")
                    .category("Sahara-Wüstentouren")
                    .departureLocation("Marrakesch")
                    .returnLocation("Marrakesch")
                    .meetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr")
                    .availability("Täglich")
                    .whatToExpect("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nWährend dieser 4-Tage-Wüstentour von Marrakesch haben Sie die Gelegenheit, die kaiserlichen Städte von Marrakesch sowie die Sahara-Wüste zu entdecken, es ist eine Mischung aus Kultur, Geschichte und UNESCO-Weltkulturerbe. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für eine unvergessliche Reise in die Sahara-Wüste und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.")
                    .build();
        }
        translations.add(deTranslation);
        
        // Save all translations
        activityTranslationRepository.saveAll(translations);
    }
    
    private void seed3DaysDesertTripFromMarrakechToFesIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            return;
        }
        
        // Always call seed3DaysDesertTripFromMarrakechToFes to ensure activity and translations are up to date
        seed3DaysDesertTripFromMarrakechToFes(saharaDesert);
    }
    
    private void seed3DaysDesertTripFromMarrakechToFes(Destination saharaDesert) {
        String slug = SlugUtil.generateSlug("3 Day Sahara Desert Trip from Marrakech to Fes");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        boolean isNew = false;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - preserve all user modifications
            activity = existingActivity.get();
            // Only update critical fields when opt-in flags are true (default: preserve admin destination/active)
            boolean needsSave = false;
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            if (needsSave) {
                activityRepository.save(activity);
            }
            // Skip creating new activity - preserve existing
            return;
        } else {
            isNew = true;
            activity = Activity.builder()
                    .title("3 Day Sahara Desert Trip from Marrakech to Fes")
                    .slug(slug)
                    .shortDescription("A short but full impression of this area of Morocco with its Mountains, gorges, valleys, Kasbahs, and Sahara desert. Not to mention the splendor of the night sky, the sunset, the sunrise, and the hospitality of the people.")
                    .fullDescription("Marrakesh is home to some of the most extraordinary structures including the Kasbah, a number of brilliant mosques, an open-air theatre, palaces, and gardens that attract tourists from all over the world. From reasonable shopping in famous Medina souks to historical sightseeing of the many museums and monuments, this place has it all. The High Atlas region of Marakkech provides access to the picturesque mountain beauty.\n\nThis 3 Day Sahara Desert Trip from Marrakech to Fes will take you on a Tour from Marrakech to Fes to explore the high atlas mountains, southern market town renowned Unesco Heritage site, or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Sahara Desert Tours to the Merzouga and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .price(new BigDecimal("89.00"))
                    .duration("3 Days")
                    .location("Marrakech to Fes")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.8"))
                    .reviewCount(1508)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(32)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Marrakech")
                    .returnLocation("Fes")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("Marrakech is home to some of the most extraordinary structures including the Kasbah, a number of brilliant mosques, an open-air theatre, palaces, and gardens that attract tourists from all over the world. From reasonable shopping in famous Medina souks to historical sightseeing of the many museums and monuments, this place has it all. The High Atlas region of Marakkech provides access to the picturesque mountain beauty.\n\nDuring this 3 Day Sahara Desert Trip from Marrakech to Fes, you get a chance to discover the imperial cities of Marrakech and Fes as well as the Sahara Desert, it's a mix of culture, history, and Unesco Heritage site. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for a memorable trip to the Sahara desert and visit the gorges of Dades and Todra, experience a camel ride and go through the sand dunes of Merzouga in a caravan trail to watch a wonderful sunset over the big dunes, spend a night in the luxury desert camp and enjoy moments with locals.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: MARRAKECH - AIT BEN HADDOU KASBAH - OUARZAZATE - ROSES VALLEY - DADES GORGES. Your driver/guide will pick you up at your Marrakech accommodation at 8 am and drive through the route passes through valleys and deserts allowing you to enjoy spectacular views from the Atlas Mountains. We will go through the Tizi n'Tichka, a pass at an altitude of 2,260 meters: the highest pass in North Africa and it has become famous for the breathtaking view it offers. Halfway, you will benefit from a free visit to the famous Kasbah Ait Ben Haddou, a fortified city classified as a UNESCO World Heritage Site. Lunch in a local restaurant overlooking the kasbah, and then we will continue our journey towards Ouarzazate known as the gate of the desert, where we will stop at the cinema studios for a short visit (optional), and then, we will continue to the heart of the city to discover another hidden gem, the kasbah Taourirte a world heritage site by UNESCO and the house of the leader Pacha el glamour, after a visit of the kasbah we will continue our journey towards our final destination for today the Dades gorges, on the road we will stop at the rises valley for a short visit to a local cooperative producing the rose water and its products. After a short drive, we will arrive at the Dades gorges where we will spend the night in a local hotel or Riad",
                            "Day 2: DADES GORGES - TODRA GORGES - ERFOUD - MERZOUGA DESERT. After breakfast at your hotel, we will start our day with a visit to the Dades Gorges, then, we will head towards Todra Gorges, the favorite place for rock climbers. After a visit to the gorges and the palm oasis of Todra, we continue our journey, following the ancient Caravan trade routes to the Sahara passing through a series of fortified villages with outstanding pre-Saharan architecture. The route to Erfoud is one of the most pleasant of all the southern routes. The city is in a dry red built of the desert and was built by the French as a central administration city. It is known for its rich black marble fossils. After Erfoud, we will continue our day heading towards the spectacular dunes of Merzouga. At our arrival, we will be welcomed by a mint tea, then, get ready for a new adventure experience, to a Berber camp in a lifetime experience, you will ride a camel through the golden dunes of Erg Chebbi, to assist to a wonderful sunset and spend a romantic night in desert camp. After dinner, we gather around the fire and enjoy the desert night, perhaps with traditional Berber drums.",
                            "Day 3: MERZOUGA - ZIZ VALLEY - CEDAR FOREST - IFRANE - FES. Wake up early before sunrise to enjoy the sunrise from the Great Dune of the Sahara Desert. Return to the hotel in dromedaries or in 4WD to meet your driver to start a new journey to Fez, which is considered to be Morocco's cultural and spiritual center with a massive history that goes right back to the 9th century. This city comprises many beautifully preserved historical buildings, including mosques, palaces, and fountains all fixed in a maze of narrow streets and passages which are interesting to explore. We will drive through the great palm grove of Ziz valley, and Errachidia, then, continues through the middle atlas mountains to reach the city of Midelt, where we will have a stop for lunch. After lunch we will head towards the cedar forest in Azrou, there, we will stop to meet the wild Barbary apes (macaque) and then continue to Ifrane, referred to as the small Switzerland of Morocco, we will stop for cafe and explore the beautiful town before driving to Fes via the green hills of the Middle Atlas. We will arrive at Fes around 6 pm in the afternoon."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(17),
                            LocalDate.now().plusDays(21),
                            LocalDate.now().plusDays(24),
                            LocalDate.now().plusDays(28),
                            LocalDate.now().plusDays(31),
                            LocalDate.now().plusDays(35),
                            LocalDate.now().plusDays(42)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.SHARED)
                    .build();
        }
        
        activityRepository.save(activity);
        
        // Create or update translations for the 3-day desert trip from Marrakech to Fes
        seed3DaysDesertTripFromMarrakechToFesTranslations(activity);
    }
    
    private void seed3DaysDesertTripFromMarrakechToFesTranslations(Activity activity) {
        // Check if translations already exist and update them, otherwise create new ones
        List<ActivityTranslation> translations = new ArrayList<>();
        
        // French translation
        Optional<ActivityTranslation> existingFr = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "fr");
        ActivityTranslation frTranslation;
        if (existingFr.isPresent()) {
            frTranslation = existingFr.get();
            frTranslation.setTitle("Circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès");
            frTranslation.setShortDescription("Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.");
            frTranslation.setFullDescription("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nCe circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès vous emmènera dans un circuit de Marrakech à Fès pour explorer les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.");
            frTranslation.setLocation("Marrakech à Fès");
            frTranslation.setCategory("Circuits du désert du Sahara");
            frTranslation.setDepartureLocation("Marrakech");
            frTranslation.setReturnLocation("Fès");
            frTranslation.setMeetingTime("15 minutes avant l'heure de départ à 8h");
            frTranslation.setAvailability("Tous les jours");
            frTranslation.setWhatToExpect("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nAu cours de ce circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès, vous aurez l'occasion de découvrir les villes impériales de Marrakech et Fès ainsi que le désert du Sahara, c'est un mélange de culture, d'histoire et de site du patrimoine de l'Unesco. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour un voyage mémorable dans le désert du Sahara et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.");
        } else {
            frTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("fr")
                    .title("Circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès")
                    .shortDescription("Une impression courte mais complète de cette région du Maroc avec ses montagnes, gorges, vallées, kasbahs et désert du Sahara. Sans oublier la splendeur du ciel nocturne, le coucher de soleil, le lever du soleil et l'hospitalité des gens.")
                    .fullDescription("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nCe circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès vous emmènera dans un circuit de Marrakech à Fès pour explorer les hautes montagnes de l'Atlas, la ville de marché du sud réputée site du patrimoine de l'Unesco, ou les kasbahs fortifiés. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour les mémorables circuits du désert du Sahara à Merzouga et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.")
                    .location("Marrakech à Fès")
                    .category("Circuits du désert du Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Fès")
                    .meetingTime("15 minutes avant l'heure de départ à 8h")
                    .availability("Tous les jours")
                    .whatToExpect("Marrakech abrite certaines des structures les plus extraordinaires, notamment la Kasbah, un certain nombre de mosquées brillantes, un théâtre en plein air, des palais et des jardins qui attirent les touristes du monde entier. Des achats raisonnables dans les souks célèbres de la Médina à la visite historique des nombreux musées et monuments, cet endroit a tout pour plaire. La région du Haut Atlas de Marrakech offre un accès à la beauté pittoresque de la montagne.\n\nAu cours de ce circuit de 3 jours dans le désert du Sahara depuis Marrakech jusqu'à Fès, vous aurez l'occasion de découvrir les villes impériales de Marrakech et Fès ainsi que le désert du Sahara, c'est un mélange de culture, d'histoire et de site du patrimoine de l'Unesco. Imprégnez-vous de la beauté de votre environnement et engagez une conversation agréable avec des habitants multilingues pour un voyage mémorable dans le désert du Sahara et visitez les gorges de Dades et Todra, vivez une balade à dos de chameau et traversez les dunes de sable de Merzouga dans une piste de caravane pour regarder un magnifique coucher de soleil sur les grandes dunes, passez une nuit dans le camp de luxe du désert et profitez de moments avec les habitants.")
                    .build();
        }
        translations.add(frTranslation);
        
        // Spanish translation
        Optional<ActivityTranslation> existingEs = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "es");
        ActivityTranslation esTranslation;
        if (existingEs.isPresent()) {
            esTranslation = existingEs.get();
            esTranslation.setTitle("Viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez");
            esTranslation.setShortDescription("Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.");
            esTranslation.setFullDescription("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nEste viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez te llevará en un tour de Marrakech a Fez para explorar las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables tours del desierto del Sahara a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.");
            esTranslation.setLocation("Marrakech a Fez");
            esTranslation.setCategory("Tours del Desierto del Sahara");
            esTranslation.setDepartureLocation("Marrakech");
            esTranslation.setReturnLocation("Fez");
            esTranslation.setMeetingTime("15 minutos antes de la hora de salida a las 8 am");
            esTranslation.setAvailability("Todos los días");
            esTranslation.setWhatToExpect("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nDurante este viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez, tendrás la oportunidad de descubrir las ciudades imperiales de Marrakech y Fez así como el desierto del Sahara, es una mezcla de cultura, historia y sitio del Patrimonio de la Unesco. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para un viaje memorable al desierto del Sahara y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.");
        } else {
            esTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("es")
                    .title("Viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez")
                    .shortDescription("Una impresión corta pero completa de esta área de Marruecos con sus montañas, gargantas, valles, Kasbahs y desierto del Sahara. Sin mencionar el esplendor del cielo nocturno, la puesta de sol, el amanecer y la hospitalidad de la gente.")
                    .fullDescription("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nEste viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez te llevará en un tour de Marrakech a Fez para explorar las altas montañas del Atlas, la ciudad comercial del sur reconocida como sitio del Patrimonio de la Unesco, o las kasbahs fortificadas. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para los memorables tours del desierto del Sahara a Merzouga y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.")
                    .location("Marrakech a Fez")
                    .category("Tours del Desierto del Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Fez")
                    .meetingTime("15 minutos antes de la hora de salida a las 8 am")
                    .availability("Todos los días")
                    .whatToExpect("Marrakech alberga algunas de las estructuras más extraordinarias, incluidas la Kasbah, varias mezquitas brillantes, un teatro al aire libre, palacios y jardines que atraen a turistas de todo el mundo. Desde compras razonables en los famosos zocos de la Medina hasta visitas históricas a los numerosos museos y monumentos, este lugar lo tiene todo. La región del Alto Atlas de Marrakech proporciona acceso a la belleza pintoresca de la montaña.\n\nDurante este viaje de 3 días al desierto del Sahara desde Marrakech hasta Fez, tendrás la oportunidad de descubrir las ciudades imperiales de Marrakech y Fez así como el desierto del Sahara, es una mezcla de cultura, historia y sitio del Patrimonio de la Unesco. Absorbe la belleza de tu entorno y participa en una conversación agradable con lugareños multilingües para un viaje memorable al desierto del Sahara y visita las gargantas de Dades y Todra, experimenta un paseo en camello y atraviesa las dunas de arena de Merzouga en un sendero de caravana para ver una maravillosa puesta de sol sobre las grandes dunas, pasa una noche en el campamento de lujo del desierto y disfruta de momentos con los lugareños.")
                    .build();
        }
        translations.add(esTranslation);
        
        // German translation
        Optional<ActivityTranslation> existingDe = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "de");
        ActivityTranslation deTranslation;
        if (existingDe.isPresent()) {
            deTranslation = existingDe.get();
            deTranslation.setTitle("3-Tage-Wüstentour von Marrakesch nach Fes");
            deTranslation.setShortDescription("Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.");
            deTranslation.setFullDescription("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nDiese 3-Tage-Wüstentour von Marrakesch nach Fes führt Sie auf eine Tour von Marrakesch nach Fes, um die hohen Atlas-Berge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs zu erkunden. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.");
            deTranslation.setLocation("Marrakesch nach Fes");
            deTranslation.setCategory("Sahara-Wüstentouren");
            deTranslation.setDepartureLocation("Marrakesch");
            deTranslation.setReturnLocation("Fes");
            deTranslation.setMeetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr");
            deTranslation.setAvailability("Täglich");
            deTranslation.setWhatToExpect("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nWährend dieser 3-Tage-Wüstentour von Marrakesch nach Fes haben Sie die Gelegenheit, die kaiserlichen Städte von Marrakesch und Fes sowie die Sahara-Wüste zu entdecken, es ist eine Mischung aus Kultur, Geschichte und UNESCO-Weltkulturerbe. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für eine unvergessliche Reise in die Sahara-Wüste und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.");
        } else {
            deTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("de")
                    .title("3-Tage-Wüstentour von Marrakesch nach Fes")
                    .shortDescription("Ein kurzer aber vollständiger Eindruck von diesem Gebiet Marokkos mit seinen Bergen, Schluchten, Tälern, Kasbahs und der Sahara-Wüste. Ganz zu schweigen von der Pracht des Nachthimmels, dem Sonnenuntergang, dem Sonnenaufgang und der Gastfreundschaft der Menschen.")
                    .fullDescription("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nDiese 3-Tage-Wüstentour von Marrakesch nach Fes führt Sie auf eine Tour von Marrakesch nach Fes, um die hohen Atlas-Berge, die südliche Marktstadt, die als UNESCO-Weltkulturerbe bekannt ist, oder die befestigten Kasbahs zu erkunden. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für die unvergesslichen Sahara-Wüstentouren nach Merzouga und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.")
                    .location("Marrakesch nach Fes")
                    .category("Sahara-Wüstentouren")
                    .departureLocation("Marrakesch")
                    .returnLocation("Fes")
                    .meetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr")
                    .availability("Täglich")
                    .whatToExpect("Marrakesch beherbergt einige der außergewöhnlichsten Strukturen, darunter die Kasbah, eine Reihe brillanter Moscheen, ein Freilufttheater, Paläste und Gärten, die Touristen aus aller Welt anziehen. Von vernünftigem Einkaufen in berühmten Medina-Souks bis hin zu historischen Sehenswürdigkeiten der vielen Museen und Denkmäler - dieser Ort hat alles zu bieten. Die Region Hoher Atlas von Marrakesch bietet Zugang zur malerischen Bergschönheit.\n\nWährend dieser 3-Tage-Wüstentour von Marrakesch nach Fes haben Sie die Gelegenheit, die kaiserlichen Städte von Marrakesch und Fes sowie die Sahara-Wüste zu entdecken, es ist eine Mischung aus Kultur, Geschichte und UNESCO-Weltkulturerbe. Saugen Sie die Lieblichkeit Ihrer Umgebung auf und führen Sie ein angenehmes Gespräch mit mehrsprachigen Einheimischen für eine unvergessliche Reise in die Sahara-Wüste und besuchen Sie die Schluchten von Dades und Todra, erleben Sie eine Kamelritt und gehen Sie durch die Sanddünen von Merzouga auf einer Karawanenspur, um einen wunderbaren Sonnenuntergang über den großen Dünen zu beobachten, verbringen Sie eine Nacht im Luxus-Wüstencamp und genießen Sie Momente mit Einheimischen.")
                    .build();
        }
        translations.add(deTranslation);
        
        // Save all translations
        activityTranslationRepository.saveAll(translations);
    }

    private void seed3DaySaharaDesertTourFromMarrakechIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            return;
        }
        
        // Always call seed3DaySaharaDesertTourFromMarrakech to ensure activity and translations are up to date
        seed3DaySaharaDesertTourFromMarrakech(saharaDesert);
    }
    
    private void seed3DaySaharaDesertTourFromMarrakech(Destination saharaDesert) {
        String slug = SlugUtil.generateSlug("3 Day Sahara Desert Tour from Marrakech");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        boolean isNew = false;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - preserve all user modifications
            activity = existingActivity.get();
            // Only update critical fields when opt-in flags are true (default: preserve admin destination/active)
            boolean needsSave = false;
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            if (needsSave) {
                activityRepository.save(activity);
            }
            // Only ensure translations exist, but don't update the activity itself
            seed3DaySaharaDesertTourFromMarrakechTranslations(activity);
            return;
        }
        
        // Create new activity
        isNew = true;
        activity = Activity.builder()
                    .title("3 Day Sahara Desert Tour from Marrakech")
                    .slug(slug)
                    .shortDescription("Experience the magic of the Sahara Desert on this 3-day tour from Marrakech. Journey through the High Atlas Mountains, visit ancient kasbahs, ride camels through golden dunes, and spend a night under the stars in a luxury desert camp.")
                    .fullDescription("Embark on an unforgettable 3-day journey from Marrakech to the Sahara Desert. This tour takes you through the stunning High Atlas Mountains, where you'll witness breathtaking landscapes and traditional Berber villages. Visit the UNESCO World Heritage site of Ait Ben Haddou, explore the dramatic Todra and Dades Gorges, and experience the magic of the Sahara Desert at Merzouga.\n\nYour adventure includes a camel trek through the golden dunes of Erg Chebbi, where you'll watch a spectacular sunset and spend a night in a luxury desert camp. Enjoy traditional Berber music around a campfire under the starry desert sky. This tour offers a perfect blend of culture, history, and natural beauty, providing an authentic Moroccan experience that you'll treasure forever.")
                    .price(new BigDecimal("99.00"))
                    .duration("3 Days")
                    .location("Marrakech to Sahara Desert")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.8"))
                    .reviewCount(1245)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(16)
                    .availableSlots(32)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("This 3-day Sahara Desert tour from Marrakech offers an incredible journey through Morocco's diverse landscapes. You'll cross the High Atlas Mountains via the Tizi n'Tichka pass, visit ancient kasbahs and fortified villages, explore dramatic gorges, and experience the magic of the Sahara Desert. The highlight is a camel trek through the golden dunes of Erg Chebbi, where you'll watch a breathtaking sunset and spend a night in a luxury desert camp under the stars. Enjoy traditional Berber hospitality, music, and cuisine throughout your journey.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance",
                            "Breakfast and dinner",
                            "Accommodation for 2 nights"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera",
                            "Warm clothing for desert night"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: Marrakech - Ait Ben Haddou - Ouarzazate - Dades Gorges. Your driver/guide will pick you up at your Marrakech accommodation at 8 am and drive through the High Atlas Mountains via the Tizi n'Tichka pass (2,260m altitude), offering spectacular views. You'll visit the famous Kasbah Ait Ben Haddou, a UNESCO World Heritage Site and filming location for many movies. After lunch, continue to Ouarzazate, known as the 'Gate of the Desert', where you'll visit the Kasbah Taourirt and cinema studios. Then journey to the Dades Gorges, passing through the Roses Valley. Dinner and overnight at a hotel in Dades Gorges.",
                            "Day 2: Dades Gorges - Todra Gorges - Merzouga Desert. After breakfast, visit the Dades Gorges and then head to Todra Gorges, a favorite spot for rock climbers with 300m high canyons. After exploring the gorges and palm oasis, continue through ancient caravan routes to Erfoud, known for its fossil-rich black marble. Arrive at Merzouga, where you'll be welcomed with mint tea. Then embark on a camel trek through the golden dunes of Erg Chebbi to watch a spectacular sunset. Arrive at your luxury desert camp for dinner, traditional Berber music around a campfire, and a night under the stars.",
                            "Day 3: Merzouga - High Atlas Mountains - Marrakech. Wake up early to watch the sunrise over the dunes. After breakfast, return to Merzouga by camel or 4WD. Begin the journey back to Marrakech, crossing the High Atlas Mountains again. Enjoy scenic views and photo stops along the way. Arrive in Marrakech in the late afternoon, where you'll be dropped off at your accommodation."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(17),
                            LocalDate.now().plusDays(21),
                            LocalDate.now().plusDays(24),
                            LocalDate.now().plusDays(28),
                            LocalDate.now().plusDays(31),
                            LocalDate.now().plusDays(35),
                            LocalDate.now().plusDays(42)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.SHARED)
                    .build();
        
        activityRepository.save(activity);
        
        // Create or update translations for the 3-day Sahara Desert tour from Marrakech
        seed3DaySaharaDesertTourFromMarrakechTranslations(activity);
    }

    private void seed3DaySaharaDesertTourFromMarrakechTranslations(Activity activity) {
        // Check if translations already exist and update them, otherwise create new ones
        List<ActivityTranslation> translations = new ArrayList<>();
        
        // French translation
        Optional<ActivityTranslation> existingFr = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "fr");
        ActivityTranslation frTranslation;
        if (existingFr.isPresent()) {
            frTranslation = existingFr.get();
            frTranslation.setTitle("Circuit de 3 jours dans le désert du Sahara depuis Marrakech");
            frTranslation.setShortDescription("Découvrez la magie du désert du Sahara lors de ce circuit de 3 jours depuis Marrakech. Traversez les montagnes du Haut Atlas, visitez d'anciennes kasbahs, faites une balade à dos de chameau à travers les dunes dorées et passez une nuit sous les étoiles dans un camp de luxe du désert.");
            frTranslation.setFullDescription("Partez pour un voyage inoubliable de 3 jours depuis Marrakech vers le désert du Sahara. Ce circuit vous emmène à travers les magnifiques montagnes du Haut Atlas, où vous découvrirez des paysages à couper le souffle et des villages berbères traditionnels. Visitez le site du patrimoine mondial de l'UNESCO d'Ait Ben Haddou, explorez les gorges dramatiques de Todra et Dades, et découvrez la magie du désert du Sahara à Merzouga.\n\nVotre aventure comprend une randonnée à dos de chameau à travers les dunes dorées d'Erg Chebbi, où vous assisterez à un coucher de soleil spectaculaire et passerez une nuit dans un camp de luxe du désert. Profitez de la musique berbère traditionnelle autour d'un feu de camp sous le ciel étoilé du désert. Ce circuit offre un mélange parfait de culture, d'histoire et de beauté naturelle, offrant une expérience marocaine authentique que vous chérirez pour toujours.");
            frTranslation.setLocation("Marrakech vers le désert du Sahara");
            frTranslation.setCategory("Circuits du désert du Sahara");
            frTranslation.setDepartureLocation("Marrakech");
            frTranslation.setReturnLocation("Marrakech");
            frTranslation.setMeetingTime("15 minutes avant l'heure de départ à 8h");
            frTranslation.setAvailability("Tous les jours");
            frTranslation.setWhatToExpect("Ce circuit de 3 jours dans le désert du Sahara depuis Marrakech offre un voyage incroyable à travers les paysages diversifiés du Maroc. Vous traverserez les montagnes du Haut Atlas via le col de Tizi n'Tichka, visiterez d'anciennes kasbahs et villages fortifiés, explorerez des gorges dramatiques et découvrirez la magie du désert du Sahara. Le point culminant est une randonnée à dos de chameau à travers les dunes dorées d'Erg Chebbi, où vous assisterez à un coucher de soleil à couper le souffle et passerez une nuit dans un camp de luxe du désert sous les étoiles. Profitez de l'hospitalité berbère traditionnelle, de la musique et de la cuisine tout au long de votre voyage.");
        } else {
            frTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("fr")
                    .title("Circuit de 3 jours dans le désert du Sahara depuis Marrakech")
                    .shortDescription("Découvrez la magie du désert du Sahara lors de ce circuit de 3 jours depuis Marrakech. Traversez les montagnes du Haut Atlas, visitez d'anciennes kasbahs, faites une balade à dos de chameau à travers les dunes dorées et passez une nuit sous les étoiles dans un camp de luxe du désert.")
                    .fullDescription("Partez pour un voyage inoubliable de 3 jours depuis Marrakech vers le désert du Sahara. Ce circuit vous emmène à travers les magnifiques montagnes du Haut Atlas, où vous découvrirez des paysages à couper le souffle et des villages berbères traditionnels. Visitez le site du patrimoine mondial de l'UNESCO d'Ait Ben Haddou, explorez les gorges dramatiques de Todra et Dades, et découvrez la magie du désert du Sahara à Merzouga.\n\nVotre aventure comprend une randonnée à dos de chameau à travers les dunes dorées d'Erg Chebbi, où vous assisterez à un coucher de soleil spectaculaire et passerez une nuit dans un camp de luxe du désert. Profitez de la musique berbère traditionnelle autour d'un feu de camp sous le ciel étoilé du désert. Ce circuit offre un mélange parfait de culture, d'histoire et de beauté naturelle, offrant une expérience marocaine authentique que vous chérirez pour toujours.")
                    .location("Marrakech vers le désert du Sahara")
                    .category("Circuits du désert du Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutes avant l'heure de départ à 8h")
                    .availability("Tous les jours")
                    .whatToExpect("Ce circuit de 3 jours dans le désert du Sahara depuis Marrakech offre un voyage incroyable à travers les paysages diversifiés du Maroc. Vous traverserez les montagnes du Haut Atlas via le col de Tizi n'Tichka, visiterez d'anciennes kasbahs et villages fortifiés, explorerez des gorges dramatiques et découvrirez la magie du désert du Sahara. Le point culminant est une randonnée à dos de chameau à travers les dunes dorées d'Erg Chebbi, où vous assisterez à un coucher de soleil à couper le souffle et passerez une nuit dans un camp de luxe du désert sous les étoiles. Profitez de l'hospitalité berbère traditionnelle, de la musique et de la cuisine tout au long de votre voyage.")
                    .build();
        }
        translations.add(frTranslation);
        
        // Spanish translation
        Optional<ActivityTranslation> existingEs = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "es");
        ActivityTranslation esTranslation;
        if (existingEs.isPresent()) {
            esTranslation = existingEs.get();
            esTranslation.setTitle("Tour de 3 días al desierto del Sahara desde Marrakech");
            esTranslation.setShortDescription("Experimenta la magia del desierto del Sahara en este tour de 3 días desde Marrakech. Viaja a través de las montañas del Alto Atlas, visita antiguas kasbahs, monta camellos a través de dunas doradas y pasa una noche bajo las estrellas en un campamento de lujo en el desierto.");
            esTranslation.setFullDescription("Embárcate en un viaje inolvidable de 3 días desde Marrakech al desierto del Sahara. Este tour te lleva a través de las impresionantes montañas del Alto Atlas, donde serás testigo de paisajes impresionantes y pueblos bereberes tradicionales. Visita el sitio del Patrimonio Mundial de la UNESCO de Ait Ben Haddou, explora los dramáticos desfiladeros de Todra y Dades, y experimenta la magia del desierto del Sahara en Merzouga.\n\nTu aventura incluye un paseo en camello a través de las dunas doradas de Erg Chebbi, donde verás una puesta de sol espectacular y pasarás una noche en un campamento de lujo en el desierto. Disfruta de música bereber tradicional alrededor de una fogata bajo el cielo estrellado del desierto. Este tour ofrece una mezcla perfecta de cultura, historia y belleza natural, proporcionando una experiencia marroquí auténtica que atesorarás para siempre.");
            esTranslation.setLocation("Marrakech al desierto del Sahara");
            esTranslation.setCategory("Tours del Desierto del Sahara");
            esTranslation.setDepartureLocation("Marrakech");
            esTranslation.setReturnLocation("Marrakech");
            esTranslation.setMeetingTime("15 minutos antes de la hora de salida a las 8 am");
            esTranslation.setAvailability("Todos los días");
            esTranslation.setWhatToExpect("Este tour de 3 días al desierto del Sahara desde Marrakech ofrece un viaje increíble a través de los diversos paisajes de Marruecos. Cruzarás las montañas del Alto Atlas a través del paso Tizi n'Tichka, visitarás antiguas kasbahs y pueblos fortificados, explorarás desfiladeros dramáticos y experimentarás la magia del desierto del Sahara. El punto culminante es un paseo en camello a través de las dunas doradas de Erg Chebbi, donde verás una puesta de sol impresionante y pasarás una noche en un campamento de lujo en el desierto bajo las estrellas. Disfruta de la hospitalidad bereber tradicional, música y cocina durante todo tu viaje.");
        } else {
            esTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("es")
                    .title("Tour de 3 días al desierto del Sahara desde Marrakech")
                    .shortDescription("Experimenta la magia del desierto del Sahara en este tour de 3 días desde Marrakech. Viaja a través de las montañas del Alto Atlas, visita antiguas kasbahs, monta camellos a través de dunas doradas y pasa una noche bajo las estrellas en un campamento de lujo en el desierto.")
                    .fullDescription("Embárcate en un viaje inolvidable de 3 días desde Marrakech al desierto del Sahara. Este tour te lleva a través de las impresionantes montañas del Alto Atlas, donde serás testigo de paisajes impresionantes y pueblos bereberes tradicionales. Visita el sitio del Patrimonio Mundial de la UNESCO de Ait Ben Haddou, explora los dramáticos desfiladeros de Todra y Dades, y experimenta la magia del desierto del Sahara en Merzouga.\n\nTu aventura incluye un paseo en camello a través de las dunas doradas de Erg Chebbi, donde verás una puesta de sol espectacular y pasarás una noche en un campamento de lujo en el desierto. Disfruta de música bereber tradicional alrededor de una fogata bajo el cielo estrellado del desierto. Este tour ofrece una mezcla perfecta de cultura, historia y belleza natural, proporcionando una experiencia marroquí auténtica que atesorarás para siempre.")
                    .location("Marrakech al desierto del Sahara")
                    .category("Tours del Desierto del Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutos antes de la hora de salida a las 8 am")
                    .availability("Todos los días")
                    .whatToExpect("Este tour de 3 días al desierto del Sahara desde Marrakech ofrece un viaje increíble a través de los diversos paisajes de Marruecos. Cruzarás las montañas del Alto Atlas a través del paso Tizi n'Tichka, visitarás antiguas kasbahs y pueblos fortificados, explorarás desfiladeros dramáticos y experimentarás la magia del desierto del Sahara. El punto culminante es un paseo en camello a través de las dunas doradas de Erg Chebbi, donde verás una puesta de sol impresionante y pasarás una noche en un campamento de lujo en el desierto bajo las estrellas. Disfruta de la hospitalidad bereber tradicional, música y cocina durante todo tu viaje.")
                    .build();
        }
        translations.add(esTranslation);
        
        // German translation
        Optional<ActivityTranslation> existingDe = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "de");
        ActivityTranslation deTranslation;
        if (existingDe.isPresent()) {
            deTranslation = existingDe.get();
            deTranslation.setTitle("3-Tage-Wüstentour ab Marrakesch");
            deTranslation.setShortDescription("Erleben Sie die Magie der Sahara-Wüste auf dieser 3-Tage-Tour ab Marrakesch. Reisen Sie durch den Hohen Atlas, besuchen Sie alte Kasbahs, reiten Sie auf Kamelen durch goldene Dünen und verbringen Sie eine Nacht unter den Sternen in einem Luxus-Wüstencamp.");
            deTranslation.setFullDescription("Begeben Sie sich auf eine unvergessliche 3-Tage-Reise von Marrakesch in die Sahara-Wüste. Diese Tour führt Sie durch die atemberaubenden Berge des Hohen Atlas, wo Sie atemberaubende Landschaften und traditionelle Berberdörfer erleben werden. Besuchen Sie die UNESCO-Welterbestätte Ait Ben Haddou, erkunden Sie die dramatischen Schluchten von Todra und Dades und erleben Sie die Magie der Sahara-Wüste in Merzouga.\n\nIhr Abenteuer beinhaltet eine Kamelritt durch die goldenen Dünen von Erg Chebbi, wo Sie einen spektakulären Sonnenuntergang beobachten und eine Nacht in einem Luxus-Wüstencamp verbringen. Genießen Sie traditionelle Berbermusik um ein Lagerfeuer unter dem sternenklaren Wüstenhimmel. Diese Tour bietet eine perfekte Mischung aus Kultur, Geschichte und natürlicher Schönheit und bietet ein authentisches marokkanisches Erlebnis, das Sie für immer schätzen werden.");
            deTranslation.setLocation("Marrakesch zur Sahara-Wüste");
            deTranslation.setCategory("Sahara-Wüstentouren");
            deTranslation.setDepartureLocation("Marrakesch");
            deTranslation.setReturnLocation("Marrakesch");
            deTranslation.setMeetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr");
            deTranslation.setAvailability("Täglich");
            deTranslation.setWhatToExpect("Diese 3-Tage-Wüstentour ab Marrakesch bietet eine unglaubliche Reise durch Marokkos vielfältige Landschaften. Sie werden den Hohen Atlas über den Tizi n'Tichka-Pass überqueren, alte Kasbahs und befestigte Dörfer besuchen, dramatische Schluchten erkunden und die Magie der Sahara-Wüste erleben. Der Höhepunkt ist eine Kamelritt durch die goldenen Dünen von Erg Chebbi, wo Sie einen atemberaubenden Sonnenuntergang beobachten und eine Nacht in einem Luxus-Wüstencamp unter den Sternen verbringen. Genießen Sie traditionelle Berbergastfreundschaft, Musik und Küche während Ihrer gesamten Reise.");
        } else {
            deTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("de")
                    .title("3-Tage-Wüstentour ab Marrakesch")
                    .shortDescription("Erleben Sie die Magie der Sahara-Wüste auf dieser 3-Tage-Tour ab Marrakesch. Reisen Sie durch den Hohen Atlas, besuchen Sie alte Kasbahs, reiten Sie auf Kamelen durch goldene Dünen und verbringen Sie eine Nacht unter den Sternen in einem Luxus-Wüstencamp.")
                    .fullDescription("Begeben Sie sich auf eine unvergessliche 3-Tage-Reise von Marrakesch in die Sahara-Wüste. Diese Tour führt Sie durch die atemberaubenden Berge des Hohen Atlas, wo Sie atemberaubende Landschaften und traditionelle Berberdörfer erleben werden. Besuchen Sie die UNESCO-Welterbestätte Ait Ben Haddou, erkunden Sie die dramatischen Schluchten von Todra und Dades und erleben Sie die Magie der Sahara-Wüste in Merzouga.\n\nIhr Abenteuer beinhaltet eine Kamelritt durch die goldenen Dünen von Erg Chebbi, wo Sie einen spektakulären Sonnenuntergang beobachten und eine Nacht in einem Luxus-Wüstencamp verbringen. Genießen Sie traditionelle Berbermusik um ein Lagerfeuer unter dem sternenklaren Wüstenhimmel. Diese Tour bietet eine perfekte Mischung aus Kultur, Geschichte und natürlicher Schönheit und bietet ein authentisches marokkanisches Erlebnis, das Sie für immer schätzen werden.")
                    .location("Marrakesch zur Sahara-Wüste")
                    .category("Sahara-Wüstentouren")
                    .departureLocation("Marrakesch")
                    .returnLocation("Marrakesch")
                    .meetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr")
                    .availability("Täglich")
                    .whatToExpect("Diese 3-Tage-Wüstentour ab Marrakesch bietet eine unglaubliche Reise durch Marokkos vielfältige Landschaften. Sie werden den Hohen Atlas über den Tizi n'Tichka-Pass überqueren, alte Kasbahs und befestigte Dörfer besuchen, dramatische Schluchten erkunden und die Magie der Sahara-Wüste erleben. Der Höhepunkt ist eine Kamelritt durch die goldenen Dünen von Erg Chebbi, wo Sie einen atemberaubenden Sonnenuntergang beobachten und eine Nacht in einem Luxus-Wüstencamp unter den Sternen verbringen. Genießen Sie traditionelle Berbergastfreundschaft, Musik und Küche während Ihrer gesamten Reise.")
                    .build();
        }
        translations.add(deTranslation);
        
        // Save all translations
        activityTranslationRepository.saveAll(translations);
    }

    private void seedBookings() {
        List<User> users = userRepository.findByRole(User.Role.ROLE_CLIENT, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Activity> activities = activityRepository.findAll();
        
        if (users.isEmpty() || activities.isEmpty()) {
            return;
        }
        
        List<Booking> bookings = new ArrayList<>(Arrays.asList(
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(0))
                        .activity(activities.get(0))
                        .bookingDate(LocalDate.now().minusDays(5))
                        .travelDate(LocalDate.now().plusDays(10))
                        .numberOfPeople(2)
                        .totalPrice(activities.get(0).getPrice().multiply(new BigDecimal("2")))
                        .status(Booking.BookingStatus.CONFIRMED)
                        .specialRequest("Vegetarian meals please")
                        .build(),
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(1))
                        .activity(activities.get(0))
                        .bookingDate(LocalDate.now().minusDays(3))
                        .travelDate(LocalDate.now().plusDays(7))
                        .numberOfPeople(1)
                        .totalPrice(activities.get(0).getPrice())
                        .status(Booking.BookingStatus.PENDING)
                        .build(),
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(2))
                        .activity(activities.get(0))
                        .bookingDate(LocalDate.now().minusDays(10))
                        .travelDate(LocalDate.now().plusDays(14))
                        .numberOfPeople(4)
                        .totalPrice(activities.get(0).getPrice().multiply(new BigDecimal("4")))
                        .status(Booking.BookingStatus.CONFIRMED)
                        .build()
        ));
        bookingRepository.saveAll(bookings);
    }
    
    private void seedReviews() {
        List<User> users = userRepository.findByRole(User.Role.ROLE_CLIENT, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Activity> activities = activityRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        
        if (users.isEmpty() || activities.isEmpty()) {
            return;
        }
        
        List<Review> reviews = new ArrayList<>(Arrays.asList(
                Review.builder()
                        .user(users.get(0))
                        .activity(activities.get(0))
                        .rating(5)
                        .comment("Amazing experience! The city tour was fantastic and our guide was very knowledgeable. Highly recommend!")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(1))
                        .activity(activities.get(0))
                        .rating(5)
                        .comment("Best city tour ever! The driver was friendly and showed us all the highlights of Marrakech.")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(2))
                        .activity(activities.get(0))
                        .rating(4)
                        .comment("Great city tour! Our guide was knowledgeable and showed us all the highlights of Marrakech.")
                        .approved(true)
                        .build()
        ));
        reviewRepository.saveAll(reviews);
        
        // Update activity ratings
        activities.forEach(activity -> {
            List<Review> activityReviews = reviewRepository.findByActivityIdAndApprovedTrue(activity.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
            if (!activityReviews.isEmpty()) {
                double avgRating = activityReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
                activity.setRatingAverage(new BigDecimal(String.format("%.2f", avgRating)));
                activity.setReviewCount(activityReviews.size());
                activityRepository.save(activity);
            }
        });
    }
    
    private void seedFavorites() {
        List<User> users = userRepository.findByRole(User.Role.ROLE_CLIENT, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<Activity> activities = activityRepository.findAll();
        
        if (users.isEmpty() || activities.isEmpty()) {
            return;
        }
        
        List<Favorite> favorites = new ArrayList<>(Arrays.asList(
                Favorite.builder()
                        .user(users.get(0))
                        .activity(activities.get(0))
                        .build(),
                Favorite.builder()
                        .user(users.get(1))
                        .activity(activities.get(0))
                        .build(),
                Favorite.builder()
                        .user(users.get(2))
                        .activity(activities.get(0))
                        .build()
        ));
        favoriteRepository.saveAll(favorites);
    }
    
    private void seed2DayDesertTourFromMarrakechIfNeeded() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination saharaDesert = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        
        if (saharaDesert == null) {
            return;
        }
        
        // Always call seed2DayDesertTourFromMarrakech to ensure activity and translations are up to date
        seed2DayDesertTourFromMarrakech(saharaDesert);
    }
    
    private void seed2DayDesertTourFromMarrakech(Destination saharaDesert) {
        String slug = SlugUtil.generateSlug("2 Day Desert Tour from Marrakech");
        Optional<Activity> existingActivity = activityRepository.findBySlug(slug);
        Activity activity;
        boolean isNew = false;
        
        if (existingActivity.isPresent()) {
            // Activity already exists - preserve all user modifications
            activity = existingActivity.get();
            // Only update critical fields when opt-in flags are true (default: preserve admin destination/active)
            boolean needsSave = false;
            if (relinkSaharaActivities
                    && (activity.getDestination() == null || !activity.getDestination().getName().equals("Sahara Desert"))) {
                activity.setDestination(saharaDesert);
                needsSave = true;
            }
            if (reactivateAllActivities && (activity.getActive() == null || !activity.getActive())) {
                activity.setActive(true);
                needsSave = true;
            }
            if (needsSave) {
                activityRepository.save(activity);
            }
            // Only ensure translations exist, but don't update the activity itself
            seed2DayDesertTourFromMarrakechTranslations(activity);
            return;
        } else {
            isNew = true;
            activity = Activity.builder()
                    .title("2 Day Desert Tour from Marrakech")
                    .slug(slug)
                    .shortDescription("Discover the Sahara desert of Zagora and the world heritage kasbahs during this 2 Day Desert Tour from Marrakech. This tour to the Sahara desert will take you beyond to explore the green palm grove, the high atlas mountain views, and the world heritage sites.")
                    .fullDescription("2-Day Desert Tour from Marrakech to Zagora Morocco offers fun, exhilarating activities for tourists and therefore we plan our activities so that tourists can experience the whole Moroccan culture while in Marrakech. The 2-Day desert tour from Marrakech to Zagora is a great example of this policy. This two-day one-night desert excursion is a group event with shared costs that brings together about 14 tourists for a fun deserts experience. This means couples or a group of travelers pay significantly less than they would if they opted to have a private Marrakech Zagora tour.\n\nA private driver and a new air-conditioned vehicle will be at your disposal to visit the main desert and Sahara highlights from Marrakech. Our Sahara Desert Tours start from Marrakech or Fes and cover all the tourist attractions including Ait Ben Haddou kasbah, the Draa valley, the palm grove of Zagora, and the camel ride experience with a stay at the desert camp in Zagora dunes.\n\nTravel in this Sahara Desert Tours through the dramatic road if Tichka pass and the wonderful views of the high atlas mountains and the old road of caravans to reach the preserved site of Ait Ben Haddou. On the road that leads there, you will discover some of the most beautiful landscapes of Morocco.")
                    .price(new BigDecimal("99.00"))
                    .duration("2 Days")
                    .location("Marrakech to Zagora")
                    .category("Sahara Desert Tours")
                    .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                    .ratingAverage(new BigDecimal("4.7"))
                    .reviewCount(1387)
                    .featured(true)
                    .active(true)
                    .maxGroupSize(14)
                    .availableSlots(28)
                    .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                    .galleryImages(new ArrayList<>(Arrays.asList(
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200",
                            "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                            "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200",
                            "https://images.unsplash.com/photo-1518548419970-58e3b4079ab2?w=1200"
                    )))
                    .availability("Everyday")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 Minutes Before Departure time 8 am")
                    .whatToExpect("Discover the Sahara desert of Zagora and the world heritage kasbahs during this 2 Day Desert Tour from Marrakech. This tour to the Sahara desert will take you beyond to explore the green palm grove, the high atlas mountain views, and the world heritage sites.\n\nA private driver and a new air-conditioned vehicle will be at your disposal to visit the main desert and Sahara highlights from Marrakech. This tour will start from Marrakech and cover all the attractions including Ait Ben Haddou kasbah, the Draa valley, the palm grove of Zagora, and the camel ride experience with a stay at the desert camp in Zagora dunes.")
                    .includedItems(new ArrayList<>(Arrays.asList(
                            "Professional Driver/ Guide",
                            "Overnight in Desert camp and Camel Ride",
                            "Transport by an air-conditioned vehicle",
                            "Hotel Pick-up and Drop-off",
                            "Transportation insurance"
                    )))
                    .excludedItems(new ArrayList<>(Arrays.asList(
                            "Lunch and Drinks",
                            "Tips and Gratuities",
                            "Any Private Expenses",
                            "Travel Insurance"
                    )))
                    .complementaries(new ArrayList<>(Arrays.asList(
                            "Comfortable shoes for walking tour",
                            "Sunscreen",
                            "Your Camera"
                    )))
                    .itinerary(new ArrayList<>(Arrays.asList(
                            "Day 1: Marrakech - Ait Ben Haddou Kasbah - Ouarazate - Zagora\n\nWe will pick you up directly from your hotel in Marrakech and drive through the route passes through valleys and deserts allowing you to enjoy spectacular views from the Atlas Mountains. We will go through the Tizi n'Tichka, a pass at an altitude of 2,260 meters: the highest pass in North Africa and it has become famous for the breathtaking view it offers. Halfway, you will benefit from a free visit to the famous Kasbah Ait Ben Haddou, a fortified city classified as a UNESCO World Heritage Site. Lunch in a local restaurant overlooking the kasbah, and then we will continue our journey towards Draa valley, next stop will be in Draa valley for some beautiful pictures. Continue to reach the palm grove of Zagora and after a welcome tea, you will start a new adventure in a camel caravan trek to the desert camp where you will spend an unforgettable night under Berber tents. Dinner will be served in the camp and enjoy moments with Berber music show that will animate your evening.",
                            "Day 2: Zagora - Agdz - Kasbah Taourirte - Marrakech\n\nEarly wake up to enjoy a beautiful sunrise over the green palm grove of Draa valley, then, ride camels back to the hotel where you will meet your driver/guide to start a new day back to Marrakech. Today, you will explore the Draa valley and our first stop will be in the KAsbah Tamnougalte in the village of Agdz, after the visit to the kasbah we will head to Ouarzazate where we will stop again to visit the cinema museum and the old house of the Pasha El Glaoui, the kasbah Taourirte is a world heritage kasbah by UNESCO. After lunch in Ouarzazat, we will drive back to Marrakech through the High Atlas Mountains and the Tichka pass. Arrive in Marrakech around 6 PM drop off at your hotel and end of our 2 Days Sahara desert tour."
                    )))
                    .availableDates(new ArrayList<>(Arrays.asList(
                            LocalDate.now().plusDays(7),
                            LocalDate.now().plusDays(10),
                            LocalDate.now().plusDays(14),
                            LocalDate.now().plusDays(17),
                            LocalDate.now().plusDays(21),
                            LocalDate.now().plusDays(24),
                            LocalDate.now().plusDays(28),
                            LocalDate.now().plusDays(31),
                            LocalDate.now().plusDays(35),
                            LocalDate.now().plusDays(42)
                    )))
                    .mapUrl("https://maps.app.goo.gl/P4Lbh74jSGAyvYch7")
                    .destination(saharaDesert)
                    .tourType(Activity.TourType.SHARED)
                    .build();
        }
        
        activityRepository.save(activity);
        
        // Create or update translations for the 2-day desert tour from Marrakech
        seed2DayDesertTourFromMarrakechTranslations(activity);
    }
    
    private void seed2DayDesertTourFromMarrakechTranslations(Activity activity) {
        // Check if translations already exist and update them, otherwise create new ones
        List<ActivityTranslation> translations = new ArrayList<>();
        
        // French translation
        Optional<ActivityTranslation> existingFr = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "fr");
        ActivityTranslation frTranslation;
        if (existingFr.isPresent()) {
            frTranslation = existingFr.get();
            frTranslation.setTitle("Circuit de 2 jours dans le désert depuis Marrakech");
            frTranslation.setShortDescription("Découvrez le désert du Sahara de Zagora et les kasbahs du patrimoine mondial lors de ce circuit de 2 jours dans le désert depuis Marrakech. Ce circuit dans le désert du Sahara vous emmènera au-delà pour explorer la palmeraie verte, les vues des montagnes du Haut Atlas et les sites du patrimoine mondial.");
            frTranslation.setFullDescription("Le circuit de 2 jours dans le désert depuis Marrakech vers Zagora au Maroc offre des activités amusantes et exaltantes pour les touristes et nous planifions donc nos activités afin que les touristes puissent vivre toute la culture marocaine à Marrakech. Le circuit de 2 jours dans le désert depuis Marrakech vers Zagora en est un excellent exemple. Cette excursion de deux jours et une nuit dans le désert est un événement de groupe avec des coûts partagés qui réunit environ 14 touristes pour une expérience de désert amusante. Cela signifie que les couples ou un groupe de voyageurs paient beaucoup moins que s'ils optaient pour un circuit privé Marrakech Zagora.\n\nUn chauffeur privé et un nouveau véhicule climatisé seront à votre disposition pour visiter les principaux points forts du désert et du Sahara depuis Marrakech. Nos circuits dans le désert du Sahara commencent depuis Marrakech ou Fès et couvrent toutes les attractions touristiques, notamment la kasbah d'Ait Ben Haddou, la vallée du Draa, la palmeraie de Zagora et l'expérience de balade à dos de chameau avec un séjour au camp du désert dans les dunes de Zagora.");
            frTranslation.setLocation("Marrakech vers Zagora");
            frTranslation.setCategory("Circuits du désert du Sahara");
            frTranslation.setDepartureLocation("Marrakech");
            frTranslation.setReturnLocation("Marrakech");
            frTranslation.setMeetingTime("15 minutes avant l'heure de départ à 8h");
            frTranslation.setAvailability("Tous les jours");
            frTranslation.setWhatToExpect("Découvrez le désert du Sahara de Zagora et les kasbahs du patrimoine mondial lors de ce circuit de 2 jours dans le désert depuis Marrakech. Ce circuit dans le désert du Sahara vous emmènera au-delà pour explorer la palmeraie verte, les vues des montagnes du Haut Atlas et les sites du patrimoine mondial.");
        } else {
            frTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("fr")
                    .title("Circuit de 2 jours dans le désert depuis Marrakech")
                    .shortDescription("Découvrez le désert du Sahara de Zagora et les kasbahs du patrimoine mondial lors de ce circuit de 2 jours dans le désert depuis Marrakech. Ce circuit dans le désert du Sahara vous emmènera au-delà pour explorer la palmeraie verte, les vues des montagnes du Haut Atlas et les sites du patrimoine mondial.")
                    .fullDescription("Le circuit de 2 jours dans le désert depuis Marrakech vers Zagora au Maroc offre des activités amusantes et exaltantes pour les touristes et nous planifions donc nos activités afin que les touristes puissent vivre toute la culture marocaine à Marrakech. Le circuit de 2 jours dans le désert depuis Marrakech vers Zagora en est un excellent exemple. Cette excursion de deux jours et une nuit dans le désert est un événement de groupe avec des coûts partagés qui réunit environ 14 touristes pour une expérience de désert amusante. Cela signifie que les couples ou un groupe de voyageurs paient beaucoup moins que s'ils optaient pour un circuit privé Marrakech Zagora.\n\nUn chauffeur privé et un nouveau véhicule climatisé seront à votre disposition pour visiter les principaux points forts du désert et du Sahara depuis Marrakech. Nos circuits dans le désert du Sahara commencent depuis Marrakech ou Fès et couvrent toutes les attractions touristiques, notamment la kasbah d'Ait Ben Haddou, la vallée du Draa, la palmeraie de Zagora et l'expérience de balade à dos de chameau avec un séjour au camp du désert dans les dunes de Zagora.")
                    .location("Marrakech vers Zagora")
                    .category("Circuits du désert du Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutes avant l'heure de départ à 8h")
                    .availability("Tous les jours")
                    .whatToExpect("Découvrez le désert du Sahara de Zagora et les kasbahs du patrimoine mondial lors de ce circuit de 2 jours dans le désert depuis Marrakech. Ce circuit dans le désert du Sahara vous emmènera au-delà pour explorer la palmeraie verte, les vues des montagnes du Haut Atlas et les sites du patrimoine mondial.")
                    .build();
        }
        translations.add(frTranslation);
        
        // Spanish translation
        Optional<ActivityTranslation> existingEs = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "es");
        ActivityTranslation esTranslation;
        if (existingEs.isPresent()) {
            esTranslation = existingEs.get();
            esTranslation.setTitle("Viaje de 2 días al desierto desde Marrakech");
            esTranslation.setShortDescription("Descubre el desierto del Sahara de Zagora y las kasbahs del patrimonio mundial durante este viaje de 2 días al desierto desde Marrakech. Este tour al desierto del Sahara te llevará más allá para explorar el palmeral verde, las vistas de las montañas del Alto Atlas y los sitios del patrimonio mundial.");
            esTranslation.setFullDescription("El viaje de 2 días al desierto desde Marrakech a Zagora, Marruecos, ofrece actividades divertidas y emocionantes para los turistas y, por lo tanto, planificamos nuestras actividades para que los turistas puedan experimentar toda la cultura marroquí mientras están en Marrakech. El tour de 2 días al desierto desde Marrakech a Zagora es un gran ejemplo de esta política. Esta excursión de dos días y una noche al desierto es un evento grupal con costos compartidos que reúne a unos 14 turistas para una experiencia divertida en el desierto. Esto significa que las parejas o un grupo de viajeros pagan significativamente menos de lo que pagarían si optaran por un tour privado de Marrakech a Zagora.\n\nUn conductor privado y un vehículo nuevo con aire acondicionado estarán a tu disposición para visitar los principales puntos destacados del desierto y del Sahara desde Marrakech. Nuestros tours del desierto del Sahara comienzan desde Marrakech o Fez y cubren todas las atracciones turísticas, incluyendo la kasbah de Ait Ben Haddou, el valle del Draa, el palmeral de Zagora y la experiencia de paseo en camello con una estancia en el campamento del desierto en las dunas de Zagora.");
            esTranslation.setLocation("Marrakech a Zagora");
            esTranslation.setCategory("Tours del Desierto del Sahara");
            esTranslation.setDepartureLocation("Marrakech");
            esTranslation.setReturnLocation("Marrakech");
            esTranslation.setMeetingTime("15 minutos antes de la hora de salida a las 8 am");
            esTranslation.setAvailability("Todos los días");
            esTranslation.setWhatToExpect("Descubre el desierto del Sahara de Zagora y las kasbahs del patrimonio mundial durante este viaje de 2 días al desierto desde Marrakech. Este tour al desierto del Sahara te llevará más allá para explorar el palmeral verde, las vistas de las montañas del Alto Atlas y los sitios del patrimonio mundial.");
        } else {
            esTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("es")
                    .title("Viaje de 2 días al desierto desde Marrakech")
                    .shortDescription("Descubre el desierto del Sahara de Zagora y las kasbahs del patrimonio mundial durante este viaje de 2 días al desierto desde Marrakech. Este tour al desierto del Sahara te llevará más allá para explorar el palmeral verde, las vistas de las montañas del Alto Atlas y los sitios del patrimonio mundial.")
                    .fullDescription("El viaje de 2 días al desierto desde Marrakech a Zagora, Marruecos, ofrece actividades divertidas y emocionantes para los turistas y, por lo tanto, planificamos nuestras actividades para que los turistas puedan experimentar toda la cultura marroquí mientras están en Marrakech. El tour de 2 días al desierto desde Marrakech a Zagora es un gran ejemplo de esta política. Esta excursión de dos días y una noche al desierto es un evento grupal con costos compartidos que reúne a unos 14 turistas para una experiencia divertida en el desierto. Esto significa que las parejas o un grupo de viajeros pagan significativamente menos de lo que pagarían si optaran por un tour privado de Marrakech a Zagora.\n\nUn conductor privado y un vehículo nuevo con aire acondicionado estarán a tu disposición para visitar los principales puntos destacados del desierto y del Sahara desde Marrakech. Nuestros tours del desierto del Sahara comienzan desde Marrakech o Fez y cubren todas las atracciones turísticas, incluyendo la kasbah de Ait Ben Haddou, el valle del Draa, el palmeral de Zagora y la experiencia de paseo en camello con una estancia en el campamento del desierto en las dunas de Zagora.")
                    .location("Marrakech a Zagora")
                    .category("Tours del Desierto del Sahara")
                    .departureLocation("Marrakech")
                    .returnLocation("Marrakech")
                    .meetingTime("15 minutos antes de la hora de salida a las 8 am")
                    .availability("Todos los días")
                    .whatToExpect("Descubre el desierto del Sahara de Zagora y las kasbahs del patrimonio mundial durante este viaje de 2 días al desierto desde Marrakech. Este tour al desierto del Sahara te llevará más allá para explorar el palmeral verde, las vistas de las montañas del Alto Atlas y los sitios del patrimonio mundial.")
                    .build();
        }
        translations.add(esTranslation);
        
        // German translation
        Optional<ActivityTranslation> existingDe = activityTranslationRepository
                .findByActivityIdAndLanguageCode(activity.getId(), "de");
        ActivityTranslation deTranslation;
        if (existingDe.isPresent()) {
            deTranslation = existingDe.get();
            deTranslation.setTitle("2-Tage-Wüstentour ab Marrakesch");
            deTranslation.setShortDescription("Entdecken Sie die Sahara-Wüste von Zagora und die Weltkulturerbe-Kasbahs während dieser 2-Tage-Wüstentour ab Marrakesch. Diese Tour zur Sahara-Wüste führt Sie darüber hinaus, um die grüne Palmenoase, die Aussichten auf das Hohe Atlasgebirge und die Weltkulturerbestätten zu erkunden.");
            deTranslation.setFullDescription("Die 2-Tage-Wüstentour von Marrakesch nach Zagora, Marokko, bietet unterhaltsame und aufregende Aktivitäten für Touristen, und daher planen wir unsere Aktivitäten so, dass Touristen die gesamte marokkanische Kultur in Marrakesch erleben können. Die 2-Tage-Wüstentour von Marrakesch nach Zagora ist ein großartiges Beispiel für diese Politik. Diese zweitägige einnächtige Wüstenexkursion ist ein Gruppenevent mit geteilten Kosten, das etwa 14 Touristen für ein unterhaltsames Wüstenerlebnis zusammenbringt. Das bedeutet, dass Paare oder eine Gruppe von Reisenden deutlich weniger zahlen, als wenn sie sich für eine private Marrakesch-Zagora-Tour entscheiden würden.\n\nEin privater Fahrer und ein neues klimatisiertes Fahrzeug stehen Ihnen zur Verfügung, um die wichtigsten Wüsten- und Sahara-Highlights von Marrakesch zu besuchen. Unsere Sahara-Wüstentouren beginnen von Marrakesch oder Fes und decken alle touristischen Attraktionen ab, einschließlich der Kasbah Ait Ben Haddou, dem Draa-Tal, der Palmenoase von Zagora und dem Kamelritt-Erlebnis mit einem Aufenthalt im Wüstencamp in den Dünen von Zagora.");
            deTranslation.setLocation("Marrakesch nach Zagora");
            deTranslation.setCategory("Sahara-Wüstentouren");
            deTranslation.setDepartureLocation("Marrakesch");
            deTranslation.setReturnLocation("Marrakesch");
            deTranslation.setMeetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr");
            deTranslation.setAvailability("Täglich");
            deTranslation.setWhatToExpect("Entdecken Sie die Sahara-Wüste von Zagora und die Weltkulturerbe-Kasbahs während dieser 2-Tage-Wüstentour ab Marrakesch. Diese Tour zur Sahara-Wüste führt Sie darüber hinaus, um die grüne Palmenoase, die Aussichten auf das Hohe Atlasgebirge und die Weltkulturerbestätten zu erkunden.");
        } else {
            deTranslation = ActivityTranslation.builder()
                    .activity(activity)
                    .languageCode("de")
                    .title("2-Tage-Wüstentour ab Marrakesch")
                    .shortDescription("Entdecken Sie die Sahara-Wüste von Zagora und die Weltkulturerbe-Kasbahs während dieser 2-Tage-Wüstentour ab Marrakesch. Diese Tour zur Sahara-Wüste führt Sie darüber hinaus, um die grüne Palmenoase, die Aussichten auf das Hohe Atlasgebirge und die Weltkulturerbestätten zu erkunden.")
                    .fullDescription("Die 2-Tage-Wüstentour von Marrakesch nach Zagora, Marokko, bietet unterhaltsame und aufregende Aktivitäten für Touristen, und daher planen wir unsere Aktivitäten so, dass Touristen die gesamte marokkanische Kultur in Marrakesch erleben können. Die 2-Tage-Wüstentour von Marrakesch nach Zagora ist ein großartiges Beispiel für diese Politik. Diese zweitägige einnächtige Wüstenexkursion ist ein Gruppenevent mit geteilten Kosten, das etwa 14 Touristen für ein unterhaltsames Wüstenerlebnis zusammenbringt. Das bedeutet, dass Paare oder eine Gruppe von Reisenden deutlich weniger zahlen, als wenn sie sich für eine private Marrakesch-Zagora-Tour entscheiden würden.\n\nEin privater Fahrer und ein neues klimatisiertes Fahrzeug stehen Ihnen zur Verfügung, um die wichtigsten Wüsten- und Sahara-Highlights von Marrakesch zu besuchen. Unsere Sahara-Wüstentouren beginnen von Marrakesch oder Fes und decken alle touristischen Attraktionen ab, einschließlich der Kasbah Ait Ben Haddou, dem Draa-Tal, der Palmenoase von Zagora und dem Kamelritt-Erlebnis mit einem Aufenthalt im Wüstencamp in den Dünen von Zagora.")
                    .location("Marrakesch nach Zagora")
                    .category("Sahara-Wüstentouren")
                    .departureLocation("Marrakesch")
                    .returnLocation("Marrakesch")
                    .meetingTime("15 Minuten vor der Abfahrtszeit um 8 Uhr")
                    .availability("Täglich")
                    .whatToExpect("Entdecken Sie die Sahara-Wüste von Zagora und die Weltkulturerbe-Kasbahs während dieser 2-Tage-Wüstentour ab Marrakesch. Diese Tour zur Sahara-Wüste führt Sie darüber hinaus, um die grüne Palmenoase, die Aussichten auf das Hohe Atlasgebirge und die Weltkulturerbestätten zu erkunden.")
                    .build();
        }
        translations.add(deTranslation);
        
        // Save all translations
        activityTranslationRepository.saveAll(translations);
    }
    
    /**
     * Syncs Marrakech Palm Grove destination + camel activity with copy from
     * https://www.tour-in-morocco.com/tour-destination/marrakech-palm-grove/
     */
    private void applyTourInMoroccoMarrakechPalmGroveDestinationPageReference() {
        destinationRepository.findBySlug(SlugUtil.generateSlug("Marrakech Palm Grove")).ifPresent(d -> {
            d.setShortDescription(PALM_GROVE_DESTINATION_SHORT);
            d.setFullDescription(PALM_GROVE_DESTINATION_FULL);
            destinationRepository.save(d);
        });

        String actSlug = SlugUtil.generateSlug("Camel Ride Experience in Marrakech");
        activityRepository.findBySlug(actSlug).ifPresent(a -> {
            a.setTitle("Camel Ride Experience in Marrakech");
            a.setShortDescription(PALM_GROVE_ACTIVITY_SHORT);
            a.setFullDescription(PALM_GROVE_ACTIVITY_FULL);
            a.setWhatToExpect(PALM_GROVE_ACTIVITY_SHORT);
            a.setCategory("Marrakech Day Trips");
            activityRepository.save(a);
        });
        System.out.println("Applied Tour-in-Morocco Marrakech Palm Grove destination / activity reference (where rows exist).");
    }

    /**
     * Syncs Ouzoud destination + flagship day trip with copy from
     * https://www.tour-in-morocco.com/tour-destination/ouzoud-waterfalls/
     */
    private void applyTourInMoroccoOuzoudDestinationPageReference() {
        destinationRepository.findBySlug(SlugUtil.generateSlug("Ouzoud Waterfalls")).ifPresent(d -> {
            d.setShortDescription(OUZOUD_DESTINATION_SHORT);
            d.setFullDescription(OUZOUD_DESTINATION_FULL);
            destinationRepository.save(d);
        });

        String actSlug = SlugUtil.generateSlug("Marrakech Day Trip to Ouzoud Waterfalls");
        activityRepository.findBySlug(actSlug).ifPresent(a -> {
            a.setTitle("Marrakech Day Trip to Ouzoud Waterfalls");
            a.setShortDescription(OUZOUD_ACTIVITY_SHORT);
            a.setFullDescription(OUZOUD_ACTIVITY_FULL);
            a.setWhatToExpect(OUZOUD_ACTIVITY_SHORT);
            a.setCategory("Marrakech Day Trips");
            activityRepository.save(a);
        });
        System.out.println("Applied Tour-in-Morocco Ouzoud destination / activity reference (where rows exist).");
    }

    /**
     * Syncs the Sahara Desert destination and its flagship tours with listing copy and promotional prices
     * from the reference page: https://www.tour-in-morocco.com/tour-destination/sahara-desert/
     */
    private void applyTourInMoroccoSaharaDestinationPageReference() {
        String saharaSlug = SlugUtil.generateSlug("Sahara Desert");
        destinationRepository.findBySlug(saharaSlug).ifPresent(d -> {
            d.setShortDescription(
                    "Shared Sahara Desert tours — Merzouga dunes, UNESCO kasbahs, camel rides, and nights under the stars.");
            d.setFullDescription(
                    "The people of this country are super friendly and welcoming. Tour in Morocco invites you to come and explore the pristine beauty of this country, Sahara, Mountains, and Beaches. For your convenience, we have designed and tailor-made amazing Moroccan trip packages and Sahara desert Tours. Let us take care of all the worries of your trips and tours to Morocco.\n\n"
                            + "The Sahara Desert of Morocco is one of the most extraordinary destinations in the world: sand dunes of Merzouga and Erg Chebbi, camel rides, luxury desert camps, and UNESCO kasbahs from the High Atlas to the golden dunes.");
            destinationRepository.save(d);
        });

        Map<String, String[]> rows = new LinkedHashMap<>();
        rows.put("2-day-desert-tour-from-marrakech", new String[]{
                "Shared Tour from Marrakech to Sahara 2 Days",
                "Discover the Sahara desert of Morocco and the world heritage kasbahs during this Shared Sahara Desert Tour from Marrakech 2 Days. This Shared Desert Tour from Marrakech will take you beyond to explore the green palm grove, the high atlas mountain views, and the world heritage sites.",
                "59.00", "80.00", "Shared Sahara Desert Tours"
        });
        rows.put("3-day-sahara-desert-tour-from-marrakech", new String[]{
                "Shared Sahara Desert Tour from Marrakech 3 Days",
                "This Shared Sahara Desert Tour from Marrakech 3 Days will take you to explore the southern site's town renowned Unesco Heritage site or fortified kasbahs. Soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for a memorable Group Trip to the Sahara desert of Morocco...",
                "80.00", "100.00", "Shared Sahara Desert Tours"
        });
        rows.put("3-day-sahara-desert-trip-from-marrakech-to-fes", new String[]{
                "Group Sahara Desert Trip from Marrakech to Fes 3 Days",
                "This Group Sahara Desert Trip from Marrakech to Fes 3 Days will take you through the Sahara Desert, explore the high atlas mountains, southern market site's renowned Unesco Heritage site, or fortified kasbahs...",
                "120.00", "160.00", "Shared Sahara Desert Tours"
        });
        rows.put("4-days-desert-trip-from-marrakesh", new String[]{
                "Shared Desert Tour from Marrakesh 4 Days",
                "Shared Desert Tour from Marrakesh 4 Days to soak up the loveliness of your surroundings and engage in a pleasant conversation with multi-lingual locals for the memorable Shared Sahara Desert Tours to the Merzouga and camel ride...",
                "99.00", "159.00", "Shared Sahara Desert Tours"
        });
        rows.put("tour-from-fes-to-marrakech-4-days", new String[]{
                "Tour from Fes to Marrakech 4 Days",
                "Do you want a real change of scenery far from all the constraints of the modern world? This adventure trip in the desert of Morocco leads you to the heart of the Sahara Desert in landscapes of beauty. Explore imperial cities, the High Atlas, Merzouga dunes, and UNESCO kasbahs on this four-day route to Marrakech.",
                "129.00", "160.00", "Sahara Desert Tours"
        });
        rows.put("tour-from-fes-to-marrakech-3-days", new String[]{
                "Tour from Fes to Marrakech 3 Days",
                "Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures including the beautiful Kasbah and UNESCO world heritage, a number of amazing Gorges and green valleys, an open-air theatre, and beautiful sand dunes attract tourists from all over the world.",
                "99.00", "140.00", "Sahara Desert Tours"
        });

        List<Activity> toSave = new ArrayList<>();
        for (Map.Entry<String, String[]> e : rows.entrySet()) {
            activityRepository.findBySlug(e.getKey()).ifPresent(a -> {
                String[] v = e.getValue();
                a.setTitle(v[0]);
                a.setShortDescription(v[1]);
                a.setPrice(new BigDecimal(v[2]));
                a.setPremiumPrice(new BigDecimal(v[3]));
                a.setBudgetPrice(new BigDecimal(v[2]));
                a.setCategory(v[4]);
                toSave.add(a);
            });
        }
        if (!toSave.isEmpty()) {
            activityRepository.saveAll(toSave);
            System.out.println("Applied Tour-in-Morocco Sahara page reference to " + toSave.size() + " activities.");
        }
    }
    
    private void seedSettings() {
        Settings settings = Settings.builder()
                .siteName("Tour Timeless")
                .logoUrl("https://via.placeholder.com/200x60?text=Tour+Timeless")
                .contactEmail("tourinmorocco.contact@gmail.com")
                .contactPhone("0661053623 | 0659915763 | 0524301729")
                .address("Rue Erraouda, 40000 Marrakesh Morocco")
                .facebookUrl("https://facebook.com/tourtimeless")
                .instagramUrl("https://instagram.com/tourtimeless")
                .twitterUrl("https://twitter.com/tourtimeless")
                .youtubeUrl("https://youtube.com/tourtimeless")
                .bannerTitle("Discover Morocco's Hidden Gems")
                .bannerSubtitle("Experience unforgettable adventures in the heart of North Africa")
                .contactPhonesJson("[{\"display\":\"0661053623\",\"tel\":\"+212661053623\"},{\"display\":\"0659915763\",\"tel\":\"+212659915763\"},{\"display\":\"0524301729\",\"tel\":\"+212524301729\"}]")
                .businessHours("Mon–Fri: 9:00–18:00 (Morocco time)")
                .build();
        settingsRepository.save(settings);
    }
}
