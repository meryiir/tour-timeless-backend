package com.tourisme.repository;

import com.tourisme.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Logged-in account: linked user_id, or same email when submitted as guest. */
    @Query(
            value = "SELECT c FROM ContactMessage c WHERE c.user.id = :userId OR (c.user IS NULL AND LOWER(TRIM(c.senderEmail)) = LOWER(TRIM(:email))) ORDER BY c.createdAt DESC",
            countQuery = "SELECT count(c) FROM ContactMessage c WHERE c.user.id = :userId OR (c.user IS NULL AND LOWER(TRIM(c.senderEmail)) = LOWER(TRIM(:email)))")
    Page<ContactMessage> findForClientInbox(@Param("userId") Long userId, @Param("email") String email, Pageable pageable);
}
