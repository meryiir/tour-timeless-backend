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
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class BookingService {
    
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final BookingMapper bookingMapper;
    private final UserNotificationService userNotificationService;
    private final BookingNotificationMailService bookingNotificationMailService;
    
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
        
        BigDecimal pricePerPerson = computePricePerPerson(activity, request);
        BigDecimal totalPrice = pricePerPerson
                .multiply(BigDecimal.valueOf(request.getNumberOfPeople()))
                .setScale(2, RoundingMode.HALF_UP);
        
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
        userNotificationService.notifyAdminsNewBooking(booking);
        bookingNotificationMailService.sendNewBookingNotificationEmail(booking);
        return bookingMapper.toResponse(booking);
    }
    
    @Transactional(readOnly = true)
    public Page<BookingResponse> getMyBookings(Pageable pageable) {
        User user = getCurrentUser();
        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(bookingMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
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
    
    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return getAllBookings(pageable, false);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(Pageable pageable, boolean includeHidden) {
        if (includeHidden) {
            return bookingRepository.findAll(pageable).map(bookingMapper::toResponse);
        }
        return bookingRepository.findByHiddenFalse(pageable).map(bookingMapper::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<BookingResponse> getBookingsByUserId(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return bookingRepository.findByUserId(userId, pageable)
                .map(bookingMapper::toResponse);
    }
    
    @Transactional
    public BookingResponse updateBookingStatus(Long id, Booking.BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        
        Booking.BookingStatus previous = booking.getStatus();
        booking.setStatus(status);
        booking = bookingRepository.save(booking);
        if (previous != status) {
            userNotificationService.notifyBookingStatus(booking.getUser(), booking, status);
        }
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public BookingResponse updateBookingHidden(Long id, boolean hidden) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        booking.setHidden(hidden);
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
    
    /**
     * Matches {@code ActivityDetailPage} pricing: budget/premium columns, private +30%, luxury vs standard.
     */
    static BigDecimal computePricePerPerson(Activity activity, BookingRequest request) {
        BigDecimal basePrice = activity.getPrice();
        BigDecimal premiumPrice = activity.getPremiumPrice() != null
                ? activity.getPremiumPrice()
                : basePrice.multiply(new BigDecimal("1.6"));
        BigDecimal budgetPrice = activity.getBudgetPrice() != null
                ? activity.getBudgetPrice()
                : basePrice;

        String tour = request.getTourType() == null ? "" : request.getTourType().trim();
        String comfort = request.getComfortLevel() == null ? "" : request.getComfortLevel().trim();

        if ("premium".equalsIgnoreCase(tour)) {
            return activity.getPrice().multiply(new BigDecimal("1.6")).setScale(2, RoundingMode.HALF_UP);
        }

        boolean isPrivate = "private".equalsIgnoreCase(tour);
        boolean isLuxury = "luxury".equalsIgnoreCase(comfort);

        BigDecimal perPerson;
        if (isPrivate) {
            if (isLuxury) {
                perPerson = premiumPrice.multiply(new BigDecimal("1.3"));
            } else {
                perPerson = budgetPrice.multiply(new BigDecimal("1.3"));
            }
        } else {
            if (isLuxury) {
                perPerson = premiumPrice;
            } else {
                perPerson = budgetPrice;
            }
        }
        return perPerson.setScale(2, RoundingMode.HALF_UP);
    }
}
