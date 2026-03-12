package com.tourisme.service;

import com.tourisme.dto.response.DashboardStatsResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.Booking;
import com.tourisme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final DestinationRepository destinationRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    
    public DashboardStatsResponse getDashboardStats() {
        Long totalUsers = userRepository.count();
        Long totalActivities = activityRepository.count();
        Long totalDestinations = destinationRepository.count();
        Long totalBookings = bookingRepository.count();
        Long totalReviews = reviewRepository.count();
        
        BigDecimal totalRevenue = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED || 
                            b.getStatus() == Booking.BookingStatus.COMPLETED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Long> bookingsByStatus = bookingRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        b -> b.getStatus().name(),
                        Collectors.counting()
                ));
        
        Map<String, Long> activitiesCountByCategory = activityRepository.findAll().stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(
                        Activity::getCategory,
                        Collectors.counting()
                ));
        
        return DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalActivities(totalActivities)
                .totalDestinations(totalDestinations)
                .totalBookings(totalBookings)
                .totalReviews(totalReviews)
                .totalRevenue(totalRevenue)
                .bookingsByStatus(bookingsByStatus)
                .activitiesCountByCategory(activitiesCountByCategory)
                .build();
    }
}
