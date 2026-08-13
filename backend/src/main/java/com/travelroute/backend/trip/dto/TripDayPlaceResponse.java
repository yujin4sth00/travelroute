package com.travelroute.backend.trip.dto;

import com.travelroute.backend.trip.TripDayPlace;

public record TripDayPlaceResponse(
        Long id,
        Long placeId,
        String placeName,
        Double lat,
        Double lng,
        Integer visitOrder,
        boolean locked
) {
    public static TripDayPlaceResponse from(TripDayPlace tripDayPlace) {
        return new TripDayPlaceResponse(
                tripDayPlace.getId(),
                tripDayPlace.getPlace().getId(),
                tripDayPlace.getPlace().getName(),
                tripDayPlace.getPlace().getLat(),
                tripDayPlace.getPlace().getLng(),
                tripDayPlace.getVisitOrder(),
                tripDayPlace.isLocked()
        );
    }
}
