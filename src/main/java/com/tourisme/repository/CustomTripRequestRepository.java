package com.tourisme.repository;

import com.tourisme.entity.CustomTripRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomTripRequestRepository extends JpaRepository<CustomTripRequest, Long> {
}

