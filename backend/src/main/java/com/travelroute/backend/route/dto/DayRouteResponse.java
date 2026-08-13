package com.travelroute.backend.route.dto;

import java.util.List;

public record DayRouteResponse(
        Integer totalDistanceM,
        Integer totalDurationSec,
        List<RouteSegmentResponse> segments
) {
}
