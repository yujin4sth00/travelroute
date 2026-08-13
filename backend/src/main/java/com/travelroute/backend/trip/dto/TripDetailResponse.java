package com.travelroute.backend.trip.dto;

import com.travelroute.backend.trip.Trip;
import java.time.LocalDate;
import java.util.List;

public record TripDetailResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        List<TripDayResponse> days
) {
    public static TripDetailResponse from(Trip trip, List<TripDayResponse> days) {
        return new TripDetailResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getStartDate(),
                trip.getEndDate(),
                days
        );
    }
}
