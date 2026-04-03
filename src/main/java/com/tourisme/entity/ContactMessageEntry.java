package com.tourisme.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_message_entries", indexes = {
        @Index(name = "idx_contact_message_entry_thread_created", columnList = "contact_message_id,created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageEntry {

    public enum Sender {
        CLIENT,
        ADMIN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_message_id", nullable = false)
    private ContactMessage contactMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sender sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

