package com.travelroute.backend.trip.dto;

import jakarta.validation.constraints.NotNull;

public record TripDayPlaceCreateRequest(
        @NotNull Long placeId
) {
}
