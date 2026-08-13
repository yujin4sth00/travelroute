package com.travelroute.backend.route.dto;

import com.travelroute.backend.route.RouteCache;
import com.travelroute.backend.route.RouteSource;

public record RouteSegmentResponse(
        Long originPlaceId,
        Long destinationPlaceId,
        Integer distanceM,
        Integer durationSec,
        String pathJson,
        RouteSource source
) {
    public static RouteSegmentResponse from(Long originPlaceId, Long destinationPlaceId, RouteCache cache) {
        return new RouteSegmentResponse(
                originPlaceId,
                destinationPlaceId,
                cache.getDistanceM(),
                cache.getDurationSec(),
                cache.getPathJson(),
                cache.getSource()
        );
    }
}
