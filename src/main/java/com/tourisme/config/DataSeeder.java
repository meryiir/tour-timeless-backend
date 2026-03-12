package com.tourisme.config;

import com.tourisme.entity.*;
import com.tourisme.repository.*;
import com.tourisme.util.BookingReferenceUtil;
import com.tourisme.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final DestinationRepository destinationRepository;
    private final ActivityRepository activityRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final SettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) {
        // Only seed users if database is empty
        if (userRepository.count() == 0) {
            seedUsers();
        }
        
        // Always seed destinations and activities (will skip if already exist due to unique constraints)
        seedDestinations();
        seedActivities();
        
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
        List<Destination> existingDestinations = destinationRepository.findAll();
        List<String> existingNames = existingDestinations.stream()
                .map(Destination::getName)
                .collect(java.util.stream.Collectors.toList());
        
        List<Destination> destinations = new ArrayList<>(Arrays.asList(
                Destination.builder()
                        .name("Sahara Desert")
                        .slug(SlugUtil.generateSlug("Sahara Desert"))
                        .shortDescription("Experience the breathtaking beauty of the world's largest hot desert")
                        .fullDescription("The Sahara Desert offers an unforgettable adventure with its endless dunes, starry nights, and rich Berber culture. Explore ancient oases, ride camels at sunset, and camp under the stars in this magnificent landscape. Our Sahara Desert Tours will take you into the heart of Morocco to discover the authenticity and hospitality of the people.")
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .country("Morocco")
                        .city("Merzouga")
                        .featured(true)
                        .build(),
                Destination.builder()
                        .name("Marrakech")
                        .slug(SlugUtil.generateSlug("Marrakech"))
                        .shortDescription("Discover the vibrant Red City with its bustling souks and historic palaces")
                        .fullDescription("Marrakech, the Red City, is a feast for the senses. Wander through the maze-like medina, visit the stunning Bahia Palace, experience the vibrant Jemaa el-Fnaa square, and indulge in authentic Moroccan cuisine. The home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces.")
                        .imageUrl("https://images.unsplash.com/photo-1539650116574-75c0c6d73a6e?w=1200")
                        .country("Morocco")
                        .city("Marrakech")
                        .featured(true)
                        .build(),
                Destination.builder()
                        .name("Atlas Mountains")
                        .slug(SlugUtil.generateSlug("Atlas Mountains"))
                        .shortDescription("Trek through stunning mountain ranges and traditional Berber villages")
                        .fullDescription("The Atlas Mountains offer spectacular hiking opportunities through rugged peaks, lush valleys, and traditional Berber villages. Experience authentic mountain culture and breathtaking panoramic views. Explore the High Atlas Mountains and 3 Valleys on our day trips from Marrakech.")
                        .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                        .country("Morocco")
                        .city("Toubkal")
                        .featured(true)
                        .build(),
                Destination.builder()
                        .name("Ouzoud Waterfalls")
                        .slug(SlugUtil.generateSlug("Ouzoud Waterfalls"))
                        .shortDescription("Marvel at Morocco's most spectacular waterfalls")
                        .fullDescription("The Ouzoud Waterfalls are among the most beautiful natural attractions in Morocco. Located in the Middle Atlas Mountains, these cascading falls offer stunning views, boat rides, and opportunities to swim in the natural pools. Experience the beauty of nature and enjoy a day trip from Marrakech to these magnificent waterfalls.")
                        .imageUrl("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=1200")
                        .country("Morocco")
                        .city("Ouzoud")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Ourika Valley")
                        .slug(SlugUtil.generateSlug("Ourika Valley"))
                        .shortDescription("Explore the beautiful valley with traditional Berber villages")
                        .fullDescription("The Ourika Valley is a stunning destination just outside Marrakech, known for its lush landscapes, traditional Berber villages, and the beautiful Ourika River. Visit local markets, enjoy traditional mint tea with Berber families, and experience the authentic mountain culture of Morocco.")
                        .imageUrl("https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=1200")
                        .country("Morocco")
                        .city("Ourika")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Marrakech Palm Grove")
                        .slug(SlugUtil.generateSlug("Marrakech Palm Grove"))
                        .shortDescription("Discover the peaceful palm grove surrounding Marrakech")
                        .fullDescription("The Marrakech Palm Grove is a vast oasis of palm trees surrounding the city, offering a peaceful escape from the bustling medina. Enjoy camel rides, quad biking, or simply relax in this beautiful natural setting.")
                        .imageUrl("https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=1200")
                        .country("Morocco")
                        .city("Marrakech")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Essaouira")
                        .slug(SlugUtil.generateSlug("Essaouira"))
                        .shortDescription("Coastal gem with historic medina and beautiful beaches")
                        .fullDescription("Essaouira is a charming coastal city known for its fortified medina, beautiful beaches, and vibrant arts scene. Explore the historic port, enjoy fresh seafood, and experience the laid-back atmosphere of this UNESCO World Heritage site.")
                        .imageUrl("https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=1200")
                        .country("Morocco")
                        .city("Essaouira")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Berber Villages")
                        .slug(SlugUtil.generateSlug("Berber Villages"))
                        .shortDescription("Experience authentic Berber culture in traditional mountain villages")
                        .fullDescription("The Berber Villages in the Atlas Mountains offer an authentic glimpse into traditional Moroccan life. Visit local families, learn about their customs, enjoy traditional meals, and experience the warm hospitality of the Berber people in their mountain homes.")
                        .imageUrl("https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=1200")
                        .country("Morocco")
                        .city("Atlas Mountains")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Agafay Desert")
                        .slug(SlugUtil.generateSlug("Agafay Desert"))
                        .shortDescription("Stone desert near Marrakech offering unique desert experiences")
                        .fullDescription("The Agafay Desert is a unique stone desert located just outside Marrakech, offering a different desert experience from the Sahara. Enjoy camel rides, quad biking, luxury desert camps, and stunning views of the Atlas Mountains. Perfect for a day trip from Marrakech.")
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .country("Morocco")
                        .city("Agafay")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Fes")
                        .slug(SlugUtil.generateSlug("Fes"))
                        .shortDescription("Explore the ancient imperial city and its historic medina")
                        .fullDescription("Fes is Morocco's cultural and spiritual capital, home to the world's oldest university and a UNESCO World Heritage medina. Discover traditional crafts, historic monuments, and authentic Moroccan culture. Explore the stunning imperial cities of Fes, Marrakech, Rabat, and Meknes.")
                        .imageUrl("https://images.unsplash.com/photo-1555993534-ee0c0e0a0c0a?w=1200")
                        .country("Morocco")
                        .city("Fes")
                        .featured(true)
                        .build(),
                Destination.builder()
                        .name("Casablanca")
                        .slug(SlugUtil.generateSlug("Casablanca"))
                        .shortDescription("Modern metropolis blending French colonial architecture with Moroccan culture")
                        .fullDescription("Casablanca is Morocco's largest city and economic hub. Visit the magnificent Hassan II Mosque, explore the Art Deco architecture, and enjoy the vibrant coastal atmosphere.")
                        .imageUrl("https://images.unsplash.com/photo-1555993534-ee0c0e0a0c0a?w=1200")
                        .country("Morocco")
                        .city("Casablanca")
                        .featured(false)
                        .build(),
                Destination.builder()
                        .name("Chefchaouen")
                        .slug(SlugUtil.generateSlug("Chefchaouen"))
                        .shortDescription("The Blue Pearl of Morocco nestled in the Rif Mountains")
                        .fullDescription("Chefchaouen, the Blue City, is famous for its blue-washed buildings and stunning mountain backdrop. Explore the charming medina, shop for local crafts, and enjoy the peaceful atmosphere of this unique destination.")
                        .imageUrl("https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200")
                        .country("Morocco")
                        .city("Chefchaouen")
                        .featured(true)
                        .build()
        ));
        
        // Filter out destinations that already exist
        List<Destination> newDestinations = destinations.stream()
                .filter(dest -> !existingNames.contains(dest.getName()))
                .collect(java.util.stream.Collectors.toList());
        
        if (!newDestinations.isEmpty()) {
            destinationRepository.saveAll(newDestinations);
        }
    }
    
    private void seedActivities() {
        List<Destination> destinations = destinationRepository.findAll();
        Destination sahara = destinations.stream().filter(d -> d.getName().equals("Sahara Desert")).findFirst().orElse(null);
        Destination marrakech = destinations.stream().filter(d -> d.getName().equals("Marrakech")).findFirst().orElse(null);
        Destination atlas = destinations.stream().filter(d -> d.getName().equals("Atlas Mountains")).findFirst().orElse(null);
        Destination fes = destinations.stream().filter(d -> d.getName().equals("Fes")).findFirst().orElse(null);
        Destination essaouira = destinations.stream().filter(d -> d.getName().equals("Essaouira")).findFirst().orElse(null);
        Destination ourika = destinations.stream().filter(d -> d.getName().equals("Ourika Valley")).findFirst().orElse(null);
        Destination agafay = destinations.stream().filter(d -> d.getName().equals("Agafay Desert")).findFirst().orElse(null);
        Destination berberVillages = destinations.stream().filter(d -> d.getName().equals("Berber Villages")).findFirst().orElse(null);
        Destination chefchaouen = destinations.stream().filter(d -> d.getName().equals("Chefchaouen")).findFirst().orElse(null);
        Destination casablanca = destinations.stream().filter(d -> d.getName().equals("Casablanca")).findFirst().orElse(null);
        
        List<Activity> activities = new ArrayList<>(Arrays.asList(
                // Tours from Fes to Marrakech
                Activity.builder()
                        .title("Tour from Fes to Marrakech 3 Days")
                        .slug(SlugUtil.generateSlug("Tour from Fes to Marrakech 3 Days"))
                        .shortDescription("Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures")
                        .fullDescription("Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces. This adventure trip in the desert of Morocco leads you to the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers.")
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
                        .availability("Everyday")
                        .departureLocation("Fes")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Experience the romance of brilliant starlight desert nights at the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers with a homestay on our Morocco Trips.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Fes, drive through Middle Atlas Mountains, visit Ifrane and Azrou, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge and Dades Valley, visit Ouarzazate and Ait Benhaddou Kasbah, overnight in Ouarzazate",
                                "Day 3: Drive through High Atlas Mountains, visit Tizi n'Tichka pass, arrive in Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(10),
                                LocalDate.now().plusDays(14)
                        )))
                        .destination(fes)
                        .build(),
                Activity.builder()
                        .title("Tour from Fes to Marrakech 4 Days")
                        .slug(SlugUtil.generateSlug("Tour from Fes to Marrakech 4 Days"))
                        .shortDescription("Book our 4 days desert Trip from Marrakesh, the home to some of the most extraordinary structures")
                        .fullDescription("Book our 4 days desert Trip from Marrakesh, the home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights at the heart of the sand dunes.")
                        .price(new BigDecimal("160.00"))
                        .duration("4 Days")
                        .location("Fes to Marrakech")
                        .category("Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(48)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Fes")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Explore the stunning imperial cities of Fes, Marrakech, Rabat, and Meknes. Marvel at the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "All meals",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Fes, drive through Middle Atlas, visit Ifrane, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge, visit Dades Valley, overnight in Dades",
                                "Day 3: Visit Ouarzazate, Ait Benhaddou Kasbah, drive through High Atlas, overnight in Marrakech",
                                "Day 4: Explore Marrakech, visit Bahia Palace, souks, Jemaa el-Fnaa square"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12),
                                LocalDate.now().plusDays(16)
                        )))
                        .destination(fes)
                        .build(),
                // Sahara Desert Tours from Marrakech
                Activity.builder()
                        .title("3 Day Sahara Desert Tour from Marrakech")
                        .slug(SlugUtil.generateSlug("3 Day Sahara Desert Tour from Marrakech"))
                        .shortDescription("This three-day private desert trip from Marrakech will take you to explore the southern market town renowned Unesco Heritage site")
                        .fullDescription("This three-day private desert trip from Marrakech will take you to explore the southern market town renowned Unesco Heritage site. Experience the romance of brilliant starlight desert nights at the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers.")
                        .price(new BigDecimal("100.00"))
                        .duration("3 Days")
                        .location("Marrakech to Sahara")
                        .category("Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(65)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Explore the southern market town renowned Unesco Heritage site. Experience the romance of brilliant starlight desert nights at the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas Mountains, visit Ait Benhaddou Kasbah, continue to Dades Valley, overnight in Dades",
                                "Day 2: Drive through Todra Gorge, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 3: Sunrise over dunes, breakfast, return to Marrakech via Ouarzazate"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(6),
                                LocalDate.now().plusDays(9),
                                LocalDate.now().plusDays(13)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("3 Day Sahara Desert Trip from Marrakech to Fes")
                        .slug(SlugUtil.generateSlug("3 Day Sahara Desert Trip from Marrakech to Fes"))
                        .shortDescription("Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures")
                        .fullDescription("Take a 3 days Sahara desert Trip from Marrakesh, the home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces. This Group Sahara Desert Trip from Marrakech to Fes 3 Days will take you through the Sahara Desert, explore the stunning imperial cities.")
                        .price(new BigDecimal("160.00"))
                        .duration("3 Days")
                        .location("Marrakech to Fes")
                        .category("Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(58)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Fes")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("This Group Sahara Desert Trip from Marrakech to Fes 3 Days will take you through the Sahara Desert, explore the stunning imperial cities of Fes, Marrakech, Rabat, and Meknes.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge and Dades Valley, visit Ouarzazate, overnight in Ouarzazate",
                                "Day 3: Drive through Middle Atlas Mountains, visit Ifrane, arrive in Fes"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(11),
                                LocalDate.now().plusDays(15)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("4 Days Desert Trip from Marrakesh")
                        .slug(SlugUtil.generateSlug("4 Days Desert Trip from Marrakesh"))
                        .shortDescription("Book our 4 days desert Trip from Marrakesh, the home to some of the most extraordinary structures")
                        .fullDescription("Book our 4 days desert Trip from Marrakesh, the home to some of the most extraordinary structures including the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights at the heart of the sand dunes.")
                        .price(new BigDecimal("159.00"))
                        .duration("4 Days")
                        .location("Marrakech to Sahara")
                        .category("Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(62)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Shared Desert Tour from Marrakesh 4 Days to soak up the loveliness of your surroundings and engage in a pleasant experience. Explore the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "All meals",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Dades Valley, overnight in Dades",
                                "Day 2: Drive through Todra Gorge, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 3: Sunrise over dunes, breakfast, drive to Ouarzazate, visit Kasbahs, overnight in Ouarzazate",
                                "Day 4: Return to Marrakech via High Atlas Mountains"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12),
                                LocalDate.now().plusDays(16)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("2 Day Desert Tour from Marrakech")
                        .slug(SlugUtil.generateSlug("2 Day Desert Tour from Marrakech"))
                        .shortDescription("Discover the Sahara desert of Zagora and the world heritage kasbahs during this 2 days desert tour from Marrakech")
                        .fullDescription("Discover the Sahara desert of Zagora and the world heritage kasbahs during this 2 days desert tour from Marrakech. This Shared Sahara Desert Tour from Marrakech 2 Days will take you to explore the southern market town and experience the romance of brilliant starlight desert nights.")
                        .price(new BigDecimal("80.00"))
                        .duration("2 Days")
                        .location("Marrakech to Zagora")
                        .category("Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(72)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Discover the Sahara desert of Zagora and the world heritage kasbahs. Experience a camel trek in Sahara and the amazing life of Berbers with a homestay on our Morocco Trips.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Zagora, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, return to Marrakech via Ouarzazate"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12)
                        )))
                        .destination(marrakech)
                        .build(),
                // Sahara Desert Activities
                Activity.builder()
                        .title("Desert Safari & Camel Trek")
                        .slug(SlugUtil.generateSlug("Desert Safari & Camel Trek"))
                        .shortDescription("Experience the magic of the Sahara with a camel trek and overnight desert camp")
                        .fullDescription("Embark on an unforgettable journey through the golden dunes of the Sahara Desert. Ride camels at sunset, enjoy traditional Berber music around a campfire, sleep under the stars in a luxury desert camp, and witness a breathtaking sunrise over the dunes.")
                        .price(new BigDecimal("150.00"))
                        .duration("2 Days / 1 Night")
                        .location("Merzouga Desert")
                        .category("Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(45)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(15)
                        .availableSlots(30)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .galleryImages(new ArrayList<>(Arrays.asList(
                                "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200",
                                "https://images.unsplash.com/photo-1509316785289-025f5b846b35?w=1200"
                        )))
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Camel trek",
                                "Overnight desert camp accommodation",
                                "Traditional dinner and breakfast",
                                "Berber music performance",
                                "Transportation from hotel"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Personal expenses",
                                "Tips"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Pickup from hotel, drive to Merzouga, camel trek at sunset, dinner at camp",
                                "Day 2: Sunrise over dunes, breakfast, return to hotel"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(10),
                                LocalDate.now().plusDays(15),
                                LocalDate.now().plusDays(20)
                        )))
                        .destination(sahara)
                        .build(),
                Activity.builder()
                        .title("Quad Biking Adventure")
                        .slug(SlugUtil.generateSlug("Quad Biking Adventure"))
                        .shortDescription("Thrilling quad bike ride through the desert dunes")
                        .fullDescription("Get your adrenaline pumping with an exciting quad biking adventure through the Sahara Desert. Navigate through sand dunes, enjoy stunning desert views, and experience the thrill of off-road desert driving.")
                        .price(new BigDecimal("80.00"))
                        .duration("2 Hours")
                        .location("Merzouga Desert")
                        .category("Adventure")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(32)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(10)
                        .availableSlots(20)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Quad bike rental",
                                "Safety equipment",
                                "Professional guide",
                                "Water"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Transportation",
                                "Personal insurance"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(12)
                        )))
                        .destination(sahara)
                        .build(),
                // Marrakech Day Trips
                Activity.builder()
                        .title("Marrakech City Tour with Driver")
                        .slug(SlugUtil.generateSlug("Marrakech City Tour with Driver"))
                        .shortDescription("Meet your driver/guide at your accommodation around 9 AM and go to explore the impressive")
                        .fullDescription("Meet your driver/guide at your accommodation around 9 AM and go to explore the impressive Marrakech. Visit the stunning Bahia Palace, explore the vibrant souks, experience the bustling Jemaa el-Fnaa square, and learn about the city's rich history and culture.")
                        .price(new BigDecimal("60.00"))
                        .duration("3 Hours")
                        .location("Marrakech")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(58)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(20)
                        .availableSlots(40)
                        .imageUrl("https://images.unsplash.com/photo-1539650116574-75c0c6d73a6e?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("9 AM at your accommodation")
                        .whatToExpect("Explore the impressive Marrakech with a professional driver/guide. Visit the stunning Bahia Palace, explore the vibrant souks, experience the bustling Jemaa el-Fnaa square.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Professional driver/guide",
                                "Transportation",
                                "Hotel pickup and drop-off"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Visit Bahia Palace",
                                "Explore the souks",
                                "Experience Jemaa el-Fnaa square",
                                "Visit Koutoubia Mosque"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Camel Ride Experience in Marrakech")
                        .slug(SlugUtil.generateSlug("Camel Ride Experience in Marrakech"))
                        .shortDescription("Experience a camel ride in the Marrakech Palm Grove")
                        .fullDescription("Experience a camel ride in the Marrakech Palm Grove. Enjoy a peaceful ride through the palm trees, learn about the local culture, and capture beautiful photos of this unique experience.")
                        .price(new BigDecimal("40.00"))
                        .duration("2 Hours")
                        .location("Marrakech Palm Grove")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.5"))
                        .reviewCount(42)
                        .featured(false)
                        .active(true)
                        .maxGroupSize(15)
                        .availableSlots(30)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("Flexible - contact for scheduling")
                        .whatToExpect("Experience a camel ride in the Marrakech Palm Grove. Enjoy a peaceful ride through the palm trees and learn about the local culture.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Camel ride",
                                "Professional guide",
                                "Transportation from hotel"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Pickup from hotel",
                                "Camel ride through palm grove",
                                "Traditional mint tea",
                                "Return to hotel"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(7)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("High Atlas Mountains and 3 Valleys Day Trip")
                        .slug(SlugUtil.generateSlug("High Atlas Mountains and 3 Valleys Day Trip"))
                        .shortDescription("Explore the High Atlas Mountains and 3 Valleys on a day trip from Marrakech")
                        .fullDescription("Explore the High Atlas Mountains and 3 Valleys on a day trip from Marrakech. Visit traditional Berber villages, enjoy stunning mountain views, experience authentic mountain culture, and enjoy a traditional lunch with a Berber family.")
                        .price(new BigDecimal("55.00"))
                        .duration("1 Day")
                        .location("Atlas Mountains")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(68)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("8 AM at your accommodation")
                        .whatToExpect("Explore the High Atlas Mountains and 3 Valleys. Visit traditional Berber villages, enjoy stunning mountain views, and experience authentic mountain culture.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Traditional lunch with Berber family",
                                "Hotel pickup and drop-off"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Departure from Marrakech",
                                "Drive through High Atlas Mountains",
                                "Visit 3 Valleys (Ourika, Asni, Imlil)",
                                "Visit traditional Berber villages",
                                "Traditional lunch with Berber family",
                                "Return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(11)
                        )))
                        .destination(atlas)
                        .build(),
                Activity.builder()
                        .title("Private Day Trip to Agafay Desert from Marrakech")
                        .slug(SlugUtil.generateSlug("Private Day Trip to Agafay Desert from Marrakech"))
                        .shortDescription("Enjoy a private day trip to the Agafay Desert from Marrakech")
                        .fullDescription("Enjoy a private day trip to the Agafay Desert from Marrakech. Experience a different desert landscape, enjoy camel rides, quad biking, and stunning views of the Atlas Mountains. Perfect for those who want a desert experience close to Marrakech.")
                        .price(new BigDecimal("70.00"))
                        .duration("1 Day")
                        .location("Agafay Desert")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(54)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(12)
                        .availableSlots(24)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("8 AM at your accommodation")
                        .whatToExpect("Experience a different desert landscape in the Agafay Desert. Enjoy camel rides, quad biking, and stunning views of the Atlas Mountains.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Camel ride",
                                "Traditional lunch",
                                "Hotel pickup and drop-off"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Departure from Marrakech",
                                "Drive to Agafay Desert",
                                "Camel ride experience",
                                "Quad biking (optional)",
                                "Traditional lunch",
                                "Return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(10)
                        )))
                        .destination(agafay)
                        .build(),
                Activity.builder()
                        .title("Marrakech Day Trip to Ourika Valley")
                        .slug(SlugUtil.generateSlug("Marrakech Day Trip to Ourika Valley"))
                        .shortDescription("Explore the beautiful Ourika Valley on a day trip from Marrakech")
                        .fullDescription("Explore the beautiful Ourika Valley on a day trip from Marrakech. Visit traditional Berber villages, enjoy the lush landscapes, experience authentic mountain culture, and enjoy a traditional lunch with a Berber family.")
                        .price(new BigDecimal("50.00"))
                        .duration("1 Day")
                        .location("Ourika Valley")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(76)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("8 AM at your accommodation")
                        .whatToExpect("Explore the beautiful Ourika Valley. Visit traditional Berber villages, enjoy the lush landscapes, and experience authentic mountain culture.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Traditional lunch with Berber family",
                                "Hotel pickup and drop-off"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Departure from Marrakech",
                                "Drive to Ourika Valley",
                                "Visit traditional Berber villages",
                                "Walk along the Ourika River",
                                "Traditional lunch with Berber family",
                                "Visit local market",
                                "Return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(11)
                        )))
                        .destination(ourika)
                        .build(),
                Activity.builder()
                        .title("Marrakech Day Trip to Essaouira")
                        .slug(SlugUtil.generateSlug("Marrakech Day Trip to Essaouira"))
                        .shortDescription("Discover the charming coastal city of Essaouira on a day trip from Marrakech")
                        .fullDescription("Discover the charming coastal city of Essaouira on a day trip from Marrakech. Explore the fortified medina, visit the historic port, enjoy fresh seafood, and experience the laid-back atmosphere of this UNESCO World Heritage site.")
                        .price(new BigDecimal("65.00"))
                        .duration("1 Day")
                        .location("Essaouira")
                        .category("Marrakech Day Trips")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(89)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Every Day")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("8 AM at your accommodation")
                        .whatToExpect("Discover the charming coastal city of Essaouira. Explore the fortified medina, visit the historic port, and enjoy fresh seafood.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in air-conditioned vehicle",
                                "Professional driver/guide",
                                "Hotel pickup and drop-off"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Departure from Marrakech",
                                "Drive to Essaouira (2.5 hours)",
                                "Explore the fortified medina",
                                "Visit the historic port",
                                "Enjoy fresh seafood lunch",
                                "Free time to explore",
                                "Return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(6),
                                LocalDate.now().plusDays(9),
                                LocalDate.now().plusDays(12)
                        )))
                        .destination(essaouira)
                        .build(),
                // Marrakech Activities
                Activity.builder()
                        .title("Marrakech City Tour")
                        .slug(SlugUtil.generateSlug("Marrakech City Tour"))
                        .shortDescription("Explore the Red City's most iconic landmarks and hidden gems")
                        .fullDescription("Discover the magic of Marrakech with a comprehensive city tour. Visit the stunning Bahia Palace, explore the vibrant souks, experience the bustling Jemaa el-Fnaa square, and learn about the city's rich history and culture.")
                        .price(new BigDecimal("60.00"))
                        .duration("4 Hours")
                        .location("Marrakech Medina")
                        .category("City Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(58)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(20)
                        .availableSlots(40)
                        .imageUrl("https://images.unsplash.com/photo-1539650116574-75c0c6d73a6e?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Professional guide",
                                "Entrance fees",
                                "Hotel pickup and drop-off"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Lunch",
                                "Personal expenses"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Visit Bahia Palace",
                                "Explore the souks",
                                "Experience Jemaa el-Fnaa square",
                                "Visit Koutoubia Mosque"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Cooking Class & Market Tour")
                        .slug(SlugUtil.generateSlug("Cooking Class & Market Tour"))
                        .shortDescription("Learn to cook authentic Moroccan dishes with a local chef")
                        .fullDescription("Immerse yourself in Moroccan cuisine with a hands-on cooking class. Start with a guided tour of the local market to select fresh ingredients, then learn to prepare traditional dishes like tagine and couscous under the guidance of a professional chef.")
                        .price(new BigDecimal("75.00"))
                        .duration("5 Hours")
                        .location("Marrakech")
                        .category("Cultural Experience")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.9"))
                        .reviewCount(28)
                        .featured(false)
                        .active(true)
                        .maxGroupSize(12)
                        .availableSlots(24)
                        .imageUrl("https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Market tour",
                                "Cooking class",
                                "All ingredients",
                                "Lunch",
                                "Recipe booklet"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Transportation"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(6),
                                LocalDate.now().plusDays(11)
                        )))
                        .destination(marrakech)
                        .build(),
                // Atlas Mountains Activities
                Activity.builder()
                        .title("Mount Toubkal Trek")
                        .slug(SlugUtil.generateSlug("Mount Toubkal Trek"))
                        .shortDescription("Conquer North Africa's highest peak")
                        .fullDescription("Challenge yourself with a trek to the summit of Mount Toubkal, North Africa's highest peak at 4,167 meters. This multi-day adventure takes you through stunning mountain landscapes, traditional Berber villages, and offers breathtaking panoramic views.")
                        .price(new BigDecimal("350.00"))
                        .duration("3 Days / 2 Nights")
                        .location("Atlas Mountains")
                        .category("Hiking & Trekking")
                        .difficultyLevel(Activity.DifficultyLevel.HARD)
                        .ratingAverage(new BigDecimal("4.5"))
                        .reviewCount(22)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(8)
                        .availableSlots(16)
                        .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Professional mountain guide",
                                "Mountain hut accommodation",
                                "All meals",
                                "Equipment rental",
                                "Transportation"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Personal gear",
                                "Travel insurance"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Trek to base camp, overnight in mountain hut",
                                "Day 2: Summit attempt, return to base camp",
                                "Day 3: Return to Imlil village"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(14),
                                LocalDate.now().plusDays(21)
                        )))
                        .destination(atlas)
                        .build(),
                Activity.builder()
                        .title("Berber Village Day Trip")
                        .slug(SlugUtil.generateSlug("Berber Village Day Trip"))
                        .shortDescription("Experience authentic Berber culture in traditional mountain villages")
                        .fullDescription("Visit traditional Berber villages nestled in the Atlas Mountains. Meet local families, learn about their way of life, enjoy a traditional lunch, and experience the warm hospitality of the Berber people.")
                        .price(new BigDecimal("55.00"))
                        .duration("6 Hours")
                        .location("Atlas Mountains")
                        .category("Cultural Experience")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(35)
                        .featured(false)
                        .active(true)
                        .maxGroupSize(15)
                        .availableSlots(30)
                        .imageUrl("https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Transportation",
                                "Local guide",
                                "Traditional lunch",
                                "Tea ceremony"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Personal expenses"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(9),
                                LocalDate.now().plusDays(16)
                        )))
                        .destination(atlas)
                        .build(),
                // Chefchaouen Activities
                Activity.builder()
                        .title("Chefchaouen Walking Tour")
                        .slug(SlugUtil.generateSlug("Chefchaouen Walking Tour"))
                        .shortDescription("Discover the Blue City's charming streets and hidden corners")
                        .fullDescription("Explore the picturesque blue-washed streets of Chefchaouen on a guided walking tour. Visit the historic kasbah, shop for local crafts, learn about the city's history, and capture stunning photos of this unique destination.")
                        .price(new BigDecimal("40.00"))
                        .duration("3 Hours")
                        .location("Chefchaouen Medina")
                        .category("City Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(42)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(18)
                        .availableSlots(36)
                        .imageUrl("https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Professional guide",
                                "Entrance fees"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Personal expenses"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(10)
                        )))
                        .destination(chefchaouen)
                        .build(),
                // Fes Activities
                Activity.builder()
                        .title("Fes Medina Guided Tour")
                        .slug(SlugUtil.generateSlug("Fes Medina Guided Tour"))
                        .shortDescription("Explore the world's largest car-free urban area")
                        .fullDescription("Discover the ancient medina of Fes, a UNESCO World Heritage site. Visit the Al-Qarawiyyin University, explore traditional tanneries, see skilled craftsmen at work, and immerse yourself in the rich cultural heritage of this imperial city.")
                        .price(new BigDecimal("65.00"))
                        .duration("5 Hours")
                        .location("Fes Medina")
                        .category("City Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(38)
                        .featured(false)
                        .active(true)
                        .maxGroupSize(15)
                        .availableSlots(30)
                        .imageUrl("https://images.unsplash.com/photo-1555993534-ee0c0e0a0c0a?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Professional guide",
                                "Entrance fees",
                                "Hotel pickup"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Lunch",
                                "Personal expenses"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(13)
                        )))
                        .destination(fes)
                        .build(),
                // Shared/Group Tours
                Activity.builder()
                        .title("Shared Tour from Marrakech to Sahara 2 Days")
                        .slug(SlugUtil.generateSlug("Shared Tour from Marrakech to Sahara 2 Days"))
                        .shortDescription("Discover the Sahara desert of Morocco and the world heritage kasbahs during this Shared Sahara Desert Tour from Marrakech 2 Days")
                        .fullDescription("Discover the Sahara desert of Morocco and the world heritage kasbahs during this Shared Sahara Desert Tour from Marrakech 2 Days. This adventure trip in the desert of Morocco leads you to the heart of the sand dunes, experience a camel trek in Sahara.")
                        .price(new BigDecimal("59.00"))
                        .duration("2 Days")
                        .location("Marrakech to Sahara")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(85)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Discover the Sahara desert of Morocco and the world heritage kasbahs. Experience a camel trek in Sahara and the amazing life of Berbers.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Zagora, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(5),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Shared Sahara Desert Tour from Marrakech 3 Days")
                        .slug(SlugUtil.generateSlug("Shared Sahara Desert Tour from Marrakech 3 Days"))
                        .shortDescription("This Shared Sahara Desert Tour from Marrakech 3 Days, will take you to explore the southern site's town renowned Unesco")
                        .fullDescription("This Shared Sahara Desert Tour from Marrakech 3 Days, will take you to explore the southern site's town renowned Unesco Heritage site. Experience the romance of brilliant starlight desert nights at the heart of the sand dunes, experience a camel trek in Sahara.")
                        .price(new BigDecimal("80.00"))
                        .duration("3 Days")
                        .location("Marrakech to Sahara")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(92)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Explore the southern site's town renowned Unesco Heritage site. Experience the romance of brilliant starlight desert nights at the heart of the sand dunes.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Dades Valley, overnight in Dades",
                                "Day 2: Drive through Todra Gorge, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 3: Sunrise over dunes, breakfast, return to Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(2),
                                LocalDate.now().plusDays(6),
                                LocalDate.now().plusDays(9),
                                LocalDate.now().plusDays(13)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Group Sahara Desert Trip from Marrakech to Fes 3 Days")
                        .slug(SlugUtil.generateSlug("Group Sahara Desert Trip from Marrakech to Fes 3 Days"))
                        .shortDescription("This Group Sahara Desert Trip from Marrakech to Fes 3 Days will take you through the Sahara Desert, explore the")
                        .fullDescription("This Group Sahara Desert Trip from Marrakech to Fes 3 Days will take you through the Sahara Desert, explore the stunning imperial cities of Fes, Marrakech, Rabat, and Meknes. Experience the romance of brilliant starlight desert nights at the heart of the sand dunes.")
                        .price(new BigDecimal("120.00"))
                        .duration("3 Days")
                        .location("Marrakech to Fes")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(78)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Fes")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("This Group Sahara Desert Trip will take you through the Sahara Desert, explore the stunning imperial cities of Fes, Marrakech, Rabat, and Meknes.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge and Dades Valley, visit Ouarzazate, overnight in Ouarzazate",
                                "Day 3: Drive through Middle Atlas Mountains, visit Ifrane, arrive in Fes"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(11),
                                LocalDate.now().plusDays(15)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Shared Desert Tour from Marrakesh 4 Days")
                        .slug(SlugUtil.generateSlug("Shared Desert Tour from Marrakesh 4 Days"))
                        .shortDescription("Shared Desert Tour from Marrakesh 4 Days to soak up the loveliness of your surroundings and engage in a pleasant")
                        .fullDescription("Shared Desert Tour from Marrakesh 4 Days to soak up the loveliness of your surroundings and engage in a pleasant experience. Explore the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights.")
                        .price(new BigDecimal("99.00"))
                        .duration("4 Days")
                        .location("Marrakech to Sahara")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.8"))
                        .reviewCount(67)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Marrakech")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Soak up the loveliness of your surroundings and engage in a pleasant experience. Explore the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "All meals",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Marrakech, drive through High Atlas, visit Ait Benhaddou, continue to Dades Valley, overnight in Dades",
                                "Day 2: Drive through Todra Gorge, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 3: Sunrise over dunes, breakfast, drive to Ouarzazate, visit Kasbahs, overnight in Ouarzazate",
                                "Day 4: Return to Marrakech via High Atlas Mountains"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12),
                                LocalDate.now().plusDays(16)
                        )))
                        .destination(marrakech)
                        .build(),
                Activity.builder()
                        .title("Shared Tour from Fes to Marrakech 4 Days")
                        .slug(SlugUtil.generateSlug("Shared Tour from Fes to Marrakech 4 Days"))
                        .shortDescription("Book our Shared Tour from Fes to Marrakech 4 Days, and travel through the stunning imperial cities of Morocco Fes")
                        .fullDescription("Book our Shared Tour from Fes to Marrakech 4 Days, and travel through the stunning imperial cities of Morocco Fes. Explore the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights at the heart of the sand dunes.")
                        .price(new BigDecimal("129.00"))
                        .duration("4 Days")
                        .location("Fes to Marrakech")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(71)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Fes")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("Travel through the stunning imperial cities of Morocco Fes. Explore the beautiful fortified Kasbahs and Medina Palaces, gorges, and the romance of brilliant starlight desert nights.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "All meals",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Fes, drive through Middle Atlas, visit Ifrane, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge, visit Dades Valley, overnight in Dades",
                                "Day 3: Visit Ouarzazate, Ait Benhaddou Kasbah, drive through High Atlas, overnight in Marrakech",
                                "Day 4: Explore Marrakech, visit Bahia Palace, souks, Jemaa el-Fnaa square"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(4),
                                LocalDate.now().plusDays(8),
                                LocalDate.now().plusDays(12),
                                LocalDate.now().plusDays(16)
                        )))
                        .destination(fes)
                        .build(),
                Activity.builder()
                        .title("Shared Tour from Fes to Marrakech 3 Days")
                        .slug(SlugUtil.generateSlug("Shared Tour from Fes to Marrakech 3 Days"))
                        .shortDescription("This adventure trip in the desert of Morocco and Shared Tour from Fes to Marrakech leads you to the heart")
                        .fullDescription("This adventure trip in the desert of Morocco and Shared Tour from Fes to Marrakech leads you to the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers with a homestay on our Morocco Trips.")
                        .price(new BigDecimal("99.00"))
                        .duration("3 Days")
                        .location("Fes to Marrakech")
                        .category("Shared Sahara Desert Tours")
                        .difficultyLevel(Activity.DifficultyLevel.MODERATE)
                        .ratingAverage(new BigDecimal("4.7"))
                        .reviewCount(83)
                        .featured(true)
                        .active(true)
                        .maxGroupSize(16)
                        .availableSlots(32)
                        .imageUrl("https://images.unsplash.com/photo-1516026672322-bc52d61a55d5?w=1200")
                        .availability("Everyday")
                        .departureLocation("Fes")
                        .returnLocation("Marrakech")
                        .meetingTime("15 Minutes Before Departure time 8 am")
                        .whatToExpect("This adventure trip in the desert of Morocco leads you to the heart of the sand dunes, experience a camel trek in Sahara and the amazing life of Berbers.")
                        .complementaries(new ArrayList<>(Arrays.asList(
                                "Transportation in shared vehicle",
                                "Professional driver/guide",
                                "Camel trek in Sahara",
                                "Overnight in desert camp",
                                "Traditional dinner and breakfast",
                                "Hotel accommodation"
                        )))
                        .itinerary(new ArrayList<>(Arrays.asList(
                                "Day 1: Departure from Fes, drive through Middle Atlas Mountains, visit Ifrane and Azrou, continue to Merzouga, camel trek at sunset, overnight in desert camp",
                                "Day 2: Sunrise over dunes, breakfast, drive through Todra Gorge and Dades Valley, visit Ouarzazate and Ait Benhaddou Kasbah, overnight in Ouarzazate",
                                "Day 3: Drive through High Atlas Mountains, visit Tizi n'Tichka pass, arrive in Marrakech"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(3),
                                LocalDate.now().plusDays(7),
                                LocalDate.now().plusDays(10),
                                LocalDate.now().plusDays(14)
                        )))
                        .destination(fes)
                        .build(),
                // Casablanca Activities
                Activity.builder()
                        .title("Hassan II Mosque & City Tour")
                        .slug(SlugUtil.generateSlug("Hassan II Mosque & City Tour"))
                        .shortDescription("Visit the magnificent Hassan II Mosque and explore Casablanca")
                        .fullDescription("Discover Casablanca's highlights including the stunning Hassan II Mosque, one of the largest mosques in the world. Explore the Art Deco architecture, visit the Corniche, and learn about the city's history and culture.")
                        .price(new BigDecimal("50.00"))
                        .duration("4 Hours")
                        .location("Casablanca")
                        .category("City Tours")
                        .difficultyLevel(Activity.DifficultyLevel.EASY)
                        .ratingAverage(new BigDecimal("4.6"))
                        .reviewCount(29)
                        .featured(false)
                        .active(true)
                        .maxGroupSize(20)
                        .availableSlots(40)
                        .imageUrl("https://images.unsplash.com/photo-1555993534-ee0c0e0a0c0a?w=1200")
                        .includedItems(new ArrayList<>(Arrays.asList(
                                "Professional guide",
                                "Mosque entrance fee",
                                "Transportation"
                        )))
                        .excludedItems(new ArrayList<>(Arrays.asList(
                                "Personal expenses"
                        )))
                        .availableDates(new ArrayList<>(Arrays.asList(
                                LocalDate.now().plusDays(1),
                                LocalDate.now().plusDays(6),
                                LocalDate.now().plusDays(12)
                        )))
                        .destination(casablanca)
                        .build()
        ));
        
        // Filter out activities that already exist (by title)
        List<Activity> existingActivities = activityRepository.findAll();
        List<String> existingActivityTitles = existingActivities.stream()
                .map(Activity::getTitle)
                .collect(java.util.stream.Collectors.toList());
        
        List<Activity> newActivities = activities.stream()
                .filter(activity -> !existingActivityTitles.contains(activity.getTitle()))
                .collect(java.util.stream.Collectors.toList());
        
        if (!newActivities.isEmpty()) {
            activityRepository.saveAll(newActivities);
        }
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
                        .travelDate(LocalDate.now().plusDays(5))
                        .numberOfPeople(2)
                        .totalPrice(activities.get(0).getPrice().multiply(new BigDecimal("2")))
                        .status(Booking.BookingStatus.CONFIRMED)
                        .specialRequest("Vegetarian meals please")
                        .build(),
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(1))
                        .activity(activities.get(2))
                        .bookingDate(LocalDate.now().minusDays(3))
                        .travelDate(LocalDate.now().plusDays(1))
                        .numberOfPeople(1)
                        .totalPrice(activities.get(2).getPrice())
                        .status(Booking.BookingStatus.PENDING)
                        .build(),
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(2))
                        .activity(activities.get(4))
                        .bookingDate(LocalDate.now().minusDays(10))
                        .travelDate(LocalDate.now().plusDays(7))
                        .numberOfPeople(4)
                        .totalPrice(activities.get(4).getPrice().multiply(new BigDecimal("4")))
                        .status(Booking.BookingStatus.CONFIRMED)
                        .build(),
                Booking.builder()
                        .bookingReference(BookingReferenceUtil.generateBookingReference())
                        .user(users.get(0))
                        .activity(activities.get(1))
                        .bookingDate(LocalDate.now().minusDays(7))
                        .travelDate(LocalDate.now().plusDays(3))
                        .numberOfPeople(2)
                        .totalPrice(activities.get(1).getPrice().multiply(new BigDecimal("2")))
                        .status(Booking.BookingStatus.COMPLETED)
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
                        .comment("Amazing experience! The camel trek at sunset was magical and the desert camp was comfortable. Highly recommend!")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(1))
                        .activity(activities.get(0))
                        .rating(5)
                        .comment("Best desert experience ever! The staff was friendly and the food was delicious.")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(2))
                        .activity(activities.get(2))
                        .rating(4)
                        .comment("Great city tour! Our guide was knowledgeable and showed us all the highlights of Marrakech.")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(0))
                        .activity(activities.get(1))
                        .rating(5)
                        .comment("Thrilling quad biking adventure! The dunes were challenging and the views were spectacular.")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(3))
                        .activity(activities.get(4))
                        .rating(4)
                        .comment("Tough but rewarding trek. The summit views were absolutely worth the effort!")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(1))
                        .activity(activities.get(6))
                        .rating(5)
                        .comment("Chefchaouen is beautiful! The blue streets are even more stunning in person.")
                        .approved(true)
                        .build(),
                Review.builder()
                        .user(users.get(2))
                        .activity(activities.get(3))
                        .rating(5)
                        .comment("Fantastic cooking class! Learned so much about Moroccan cuisine and the food was delicious.")
                        .approved(false)
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
                        .activity(activities.get(4))
                        .build(),
                Favorite.builder()
                        .user(users.get(0))
                        .activity(activities.get(6))
                        .build(),
                Favorite.builder()
                        .user(users.get(1))
                        .activity(activities.get(0))
                        .build(),
                Favorite.builder()
                        .user(users.get(1))
                        .activity(activities.get(2))
                        .build(),
                Favorite.builder()
                        .user(users.get(2))
                        .activity(activities.get(1))
                        .build()
        ));
        favoriteRepository.saveAll(favorites);
    }
    
    private void seedSettings() {
        Settings settings = Settings.builder()
                .siteName("Tour Timeless")
                .logoUrl("https://via.placeholder.com/200x60?text=Tour+Timeless")
                .contactEmail("info@tourtimeless.com")
                .contactPhone("+212 6XX XXX XXX")
                .address("123 Tourism Street, Marrakech, Morocco")
                .facebookUrl("https://facebook.com/tourtimeless")
                .instagramUrl("https://instagram.com/tourtimeless")
                .twitterUrl("https://twitter.com/tourtimeless")
                .youtubeUrl("https://youtube.com/tourtimeless")
                .bannerTitle("Discover Morocco's Hidden Gems")
                .bannerSubtitle("Experience unforgettable adventures in the heart of North Africa")
                .build();
        settingsRepository.save(settings);
    }
}
