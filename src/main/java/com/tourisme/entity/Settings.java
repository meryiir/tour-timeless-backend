package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(length = 255)
    private String siteName;
    
    @Column(length = 500)
    private String logoUrl;
    
    @Column(length = 255)
    private String contactEmail;
    
    @Column(length = 255)
    private String contactPhone;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 500)
    private String facebookUrl;
    
    @Column(length = 500)
    private String instagramUrl;
    
    @Column(length = 500)
    private String twitterUrl;
    
    @Column(length = 500)
    private String youtubeUrl;
    
    @Column(length = 255)
    private String bannerTitle;
    
    @Column(length = 500)
    private String bannerSubtitle;

    @Column(columnDefinition = "TEXT")
    private String mapEmbedUrl;

    @Column(columnDefinition = "TEXT")
    private String contactPhonesJson;

    @Column(columnDefinition = "TEXT")
    private String businessHours;

    @Column(columnDefinition = "TEXT")
    private String aboutContentJson;

    @OneToMany(mappedBy = "settings", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SettingsTranslation> translations = new ArrayList<>();
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
