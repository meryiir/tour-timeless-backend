package com.tourisme.repository;

import com.tourisme.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {

    Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);
}
