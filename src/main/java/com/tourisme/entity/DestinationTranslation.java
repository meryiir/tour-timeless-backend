package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "destination_translations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"destination_id", "language_code"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DestinationTranslation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private Destination destination;
    
    @Column(nullable = false, length = 10)
    private String languageCode;
    
    @Column(nullable = false, length = 255)
    private String name;
    
    @Column(length = 500)
    private String shortDescription;
    
    @Column(columnDefinition = "TEXT")
    private String fullDescription;
}
