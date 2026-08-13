package com.travelroute.backend.place.dto;

public record PlaceSearchResult(
        String kakaoPlaceId,
        String name,
        String address,
        Double lat,
        Double lng,
        String category
) {
}
