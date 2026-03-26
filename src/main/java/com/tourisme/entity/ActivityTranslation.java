package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_translations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"activity_id", "language_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityTranslation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;
    
    @Column(nullable = false, length = 10)
    private String languageCode;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(length = 500)
    private String shortDescription;
    
    @Column(columnDefinition = "TEXT")
    private String fullDescription;
    
    @Column(length = 255)
    private String location;
    
    @Column(length = 100)
    private String category;
    
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
}
