package com.travelroute.backend.place.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlaceCreateRequest(
        @NotBlank String name,
        String address,
        @NotNull Double lat,
        @NotNull Double lng,
        String category,
        String memo,
        String kakaoPlaceId
) {
}
