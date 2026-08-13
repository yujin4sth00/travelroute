package com.travelroute.backend.trip.dto;

public record TripDayUpdateRequest(
        Long startPlaceId,
        Long endPlaceId
) {
}
