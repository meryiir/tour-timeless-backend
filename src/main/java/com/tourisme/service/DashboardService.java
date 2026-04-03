package com.tourisme.service;

import com.tourisme.dto.response.DashboardStatsResponse;
import com.tourisme.entity.Booking;
import com.tourisme.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        BigDecimal totalRevenue = bookingRepository.sumTotalPriceByStatuses(
                List.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.COMPLETED));
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Map<String, Long> bookingsByStatus = new HashMap<>();
        for (Object[] row : bookingRepository.countBookingsGroupedByStatus()) {
            Booking.BookingStatus status = (Booking.BookingStatus) row[0];
            Long cnt = (Long) row[1];
            bookingsByStatus.put(status.name(), cnt);
        }

        Map<String, Long> activitiesCountByCategory = new HashMap<>();
        for (Object[] row : activityRepository.countActivitiesGroupedByCategory()) {
            String category = (String) row[0];
            Long cnt = (Long) row[1];
            activitiesCountByCategory.put(category, cnt);
        }

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
