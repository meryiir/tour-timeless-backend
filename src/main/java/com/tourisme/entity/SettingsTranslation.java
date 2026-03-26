package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settings_translations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"settings_id", "language_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsTranslation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settings_id", nullable = false)
    private Settings settings;
    
    @Column(nullable = false, length = 10)
    private String languageCode;
    
    @Column(length = 255)
    private String siteName;
    
    @Column(length = 255)
    private String bannerTitle;
    
    @Column(length = 500)
    private String bannerSubtitle;
    
    @Column(columnDefinition = "TEXT")
    private String address;
}
