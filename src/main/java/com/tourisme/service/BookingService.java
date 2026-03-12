package com.tourisme.service;

import com.tourisme.dto.request.BookingRequest;
import com.tourisme.dto.response.BookingResponse;
import com.tourisme.entity.Activity;
import com.tourisme.entity.Booking;
import com.tourisme.entity.User;
import com.tourisme.exception.BadRequestException;
import com.tourisme.exception.ResourceNotFoundException;
import com.tourisme.mapper.BookingMapper;
import com.tourisme.repository.ActivityRepository;
import com.tourisme.repository.BookingRepository;
import com.tourisme.repository.UserRepository;
import com.tourisme.util.BookingReferenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final BookingMapper bookingMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
    
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User user = getCurrentUser();
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with id: " + request.getActivityId()));
        
        if (!activity.getActive()) {
            throw new BadRequestException("Activity is not available");
        }
        
        if (request.getNumberOfPeople() > activity.getMaxGroupSize()) {
            throw new BadRequestException("Number of people exceeds maximum group size");
        }
        
        BigDecimal totalPrice = activity.getPrice().multiply(BigDecimal.valueOf(request.getNumberOfPeople()));
        
        String bookingReference;
        do {
            bookingReference = BookingReferenceUtil.generateBookingReference();
        } while (bookingRepository.existsByBookingReference(bookingReference));
        
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .user(user)
                .activity(activity)
                .bookingDate(LocalDate.now())
                .travelDate(request.getTravelDate())
                .numberOfPeople(request.getNumberOfPeople())
                .totalPrice(totalPrice)
                .status(Booking.BookingStatus.PENDING)
                .specialRequest(request.getSpecialRequest())
                .build();
        
        booking = bookingRepository.save(booking);
        return bookingMapper.toResponse(booking);
    }
    
    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        User user = getCurrentUser();
        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(bookingMapper::toResponse);
    }
    
    public BookingResponse getBookingById(Long id) {
        User user = getCurrentUser();
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        if (!booking.getUser().getId().equals(user.getId()) && 
            !user.getRole().equals(User.Role.ROLE_ADMIN)) {
            throw new BadRequestException("You don't have access to this booking");
        }
        
        return bookingMapper.toResponse(booking);
    }
    
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(bookingMapper::toResponse);
    }
    
    @Transactional
    public BookingResponse updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        booking.setStatus(status);
        booking = bookingRepository.save(booking);
        return bookingMapper.toResponse(booking);
    }
    
    @Transactional
    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new ResourceNotFoundException("Booking not found with id: " + id);
        }
        bookingRepository.deleteById(id);
    }
}
