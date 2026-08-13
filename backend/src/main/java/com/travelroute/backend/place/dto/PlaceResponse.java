package com.travelroute.backend.place.dto;

import com.travelroute.backend.place.Place;
import java.time.LocalDateTime;

public record PlaceResponse(
        Long id,
        String name,
        String address,
        Double lat,
        Double lng,
        String category,
        String memo,
        String kakaoPlaceId,
        LocalDateTime createdAt
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getName(),
                place.getAddress(),
                place.getLat(),
                place.getLng(),
                place.getCategory(),
                place.getMemo(),
                place.getKakaoPlaceId(),
                place.getCreatedAt()
        );
    }
}
