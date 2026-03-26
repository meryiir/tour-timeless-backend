package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activity_slug", columnList = "slug", unique = true),
    @Index(name = "idx_activity_destination", columnList = "destination_id"),
    @Index(name = "idx_activity_featured", columnList = "featured"),
    @Index(name = "idx_activity_active", columnList = "active"),
    @Index(name = "idx_activity_tour_type", columnList = "tour_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, unique = true, length = 255)
    private String slug;
    
    @Column(length = 500)
    private String shortDescription;
    
    @Column(columnDefinition = "TEXT")
    private String fullDescription;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal premiumPrice;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal budgetPrice;
    
    @Column(length = 50)
    private String duration;
    
    @Column(length = 255)
    private String location;
    
    @Column(length = 100)
    private String category;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifficultyLevel difficultyLevel;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TourType tourType = TourType.SHARED;
    
    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAverage = BigDecimal.ZERO;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer maxGroupSize = 20;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer availableSlots = 50;
    
    @Column(length = 500)
    private String imageUrl;
    
    @ElementCollection
    @CollectionTable(name = "activity_gallery_images", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "activity_included_items", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "item")
    @Builder.Default
    private List<String> includedItems = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "activity_excluded_items", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "item")
    @Builder.Default
    private List<String> excludedItems = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "activity_itinerary", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "item", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> itinerary = new ArrayList<>();
    
    @ElementCollection
    @CollectionTable(name = "activity_available_dates", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "available_date")
    @Builder.Default
    private List<LocalDate> availableDates = new ArrayList<>();
    
    @Column(length = 255)
    private String departureLocation;
    
    @Column(length = 255)
    private String returnLocation;
    
    @Column(length = 255)
    private String meetingTime;
    
    @Column(length = 100)
    private String availability;
    
    @Column(columnDefinition = "TEXT")
    private String whatToExpect;
    
    @ElementCollection
    @CollectionTable(name = "activity_complementaries", joinColumns = @JoinColumn(name = "activity_id"))
    @Column(name = "item")
    @Builder.Default
    private List<String> complementaries = new ArrayList<>();
    
    @Column(length = 500)
    private String mapUrl;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;
    
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();
    
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();
    
    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ActivityTranslation> translations = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum DifficultyLevel {
        EASY,
        MODERATE,
        HARD,
        EXTREME
    }
    
    public enum TourType {
        PRIVATE,
        SHARED
    }
}
