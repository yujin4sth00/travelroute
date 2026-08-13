package com.travelroute.backend.trip.dto;

import com.travelroute.backend.trip.TripDay;
import java.time.LocalDate;
import java.util.List;

public record TripDayResponse(
        Long id,
        Integer dayNumber,
        LocalDate date,
        Long startPlaceId,
        Long endPlaceId,
        List<TripDayPlaceResponse> places
) {
    public static TripDayResponse from(TripDay tripDay, List<TripDayPlaceResponse> places) {
        return new TripDayResponse(
                tripDay.getId(),
                tripDay.getDayNumber(),
                tripDay.getDate(),
                tripDay.getStartPlace() != null ? tripDay.getStartPlace().getId() : null,
                tripDay.getEndPlace() != null ? tripDay.getEndPlace().getId() : null,
                places
        );
    }
}
