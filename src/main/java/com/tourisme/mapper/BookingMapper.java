package com.tourisme.mapper;

import com.tourisme.dto.response.BookingResponse;
import com.tourisme.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingMapper {
    
    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    
    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }
        
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .user(userMapper.toResponse(booking.getUser()))
                .activity(activityMapper.toSummaryResponse(booking.getActivity()))
                .bookingDate(booking.getBookingDate())
                .travelDate(booking.getTravelDate())
                .numberOfPeople(booking.getNumberOfPeople())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .specialRequest(booking.getSpecialRequest())
                .hidden(Boolean.TRUE.equals(booking.getHidden()))
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
