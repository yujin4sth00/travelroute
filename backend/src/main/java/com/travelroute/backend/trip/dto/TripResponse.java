package com.travelroute.backend.trip.dto;

import com.travelroute.backend.trip.Trip;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                trip.getCreatedAt()
        );
    }
}
