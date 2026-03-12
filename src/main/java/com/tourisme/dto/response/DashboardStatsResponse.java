package com.tourisme.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long totalActivities;
    private Long totalDestinations;
    private Long totalBookings;
    private Long totalReviews;
    private BigDecimal totalRevenue;
    private Map<String, Long> bookingsByStatus;
    private Map<String, Long> activitiesCountByCategory;
}
