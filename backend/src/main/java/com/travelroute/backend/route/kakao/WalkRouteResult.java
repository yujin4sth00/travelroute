package com.travelroute.backend.route.kakao;

import com.travelroute.backend.route.RouteCoordinate;
import java.util.List;

public record WalkRouteResult(int distanceM, int durationSec, List<RouteCoordinate> path) {
}
