package com.travelroute.backend.trip.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TripCreateRequest(
        @NotBlank String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
