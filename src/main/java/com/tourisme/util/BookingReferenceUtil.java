package com.tourisme.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class BookingReferenceUtil {
    
    private static final String PREFIX = "TOUR";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();
    
    public static String generateBookingReference() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomPart = String.format("%04d", RANDOM.nextInt(10000));
        return PREFIX + datePart + randomPart;
    }
}
