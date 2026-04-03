package com.tourisme.repository;

import com.tourisme.entity.ContactMessageEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageEntryRepository extends JpaRepository<ContactMessageEntry, Long> {
    List<ContactMessageEntry> findByContactMessage_IdOrderByCreatedAtAsc(Long contactMessageId);
}

