package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "destinations", indexes = {
    @Index(name = "idx_destination_slug", columnList = "slug", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Destination {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(nullable = false, unique = true, length = 255)
    private String slug;
    
    @Column(length = 500)
    private String shortDescription;
    
    @Column(columnDefinition = "TEXT")
    private String fullDescription;
    
    @Column(length = 500)
    private String imageUrl;
    
    @Column(length = 100)
    private String country;
    
    @Column(length = 100)
    private String city;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean featured = false;
    
    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Activity> activities = new ArrayList<>();
    
    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DestinationTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "destination", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<DestinationPageCard> pageCards = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
