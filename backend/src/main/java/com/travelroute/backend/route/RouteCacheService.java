package com.travelroute.backend.route;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.route.dto.RouteSegmentResponse;
import com.travelroute.backend.route.kakao.KakaoWalkClient;
import com.travelroute.backend.route.kakao.WalkRouteResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 두 장소 간 도보 경로를 캐시 우선으로 조회한다.
 * 캐시가 없을 때만 Kakao 도보 경로 API를 호출하고, 실패하면 Haversine 직선거리로 폴백해서 캐시에 남긴다.
 */
@Service
@RequiredArgsConstructor
public class RouteCacheService {

    private static final double AVERAGE_WALK_SPEED_M_PER_SEC = 1.1; // 도보 평균 속도 약 4km/h 가정

    private final RouteCacheRepository routeCacheRepository;
    private final KakaoWalkClient kakaoWalkClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public RouteSegmentResponse getOrFetchSegment(Place origin, Place destination) {
        Optional<RouteCache> cached = routeCacheRepository
                .findByOriginPlaceIdAndDestinationPlaceId(origin.getId(), destination.getId());
        if (cached.isPresent()) {
            return RouteSegmentResponse.from(origin.getId(), destination.getId(), cached.get());
        }

        RouteCoordinate originCoord = new RouteCoordinate(origin.getLat(), origin.getLng());
        RouteCoordinate destinationCoord = new RouteCoordinate(destination.getLat(), destination.getLng());

        RouteCache routeCache = kakaoWalkClient.fetchWalkingRoute(originCoord, destinationCoord)
                .map(result -> toKakaoWalkCache(origin.getId(), destination.getId(), result))
                .orElseGet(() -> toHaversineFallbackCache(origin.getId(), destination.getId(), originCoord, destinationCoord));

        routeCacheRepository.save(routeCache);
        return RouteSegmentResponse.from(origin.getId(), destination.getId(), routeCache);
    }

    private RouteCache toKakaoWalkCache(Long originPlaceId, Long destinationPlaceId, WalkRouteResult result) {
        return RouteCache.builder()
                .originPlaceId(originPlaceId)
                .destinationPlaceId(destinationPlaceId)
                .distanceM(result.distanceM())
                .durationSec(result.durationSec())
                .pathJson(toJson(result.path()))
                .source(RouteSource.KAKAO_WALK)
                .build();
    }

    private RouteCache toHaversineFallbackCache(Long originPlaceId, Long destinationPlaceId,
                                                 RouteCoordinate origin, RouteCoordinate destination) {
        double distance = HaversineCalculator.distanceMeters(origin.lat(), origin.lng(), destination.lat(), destination.lng());
        int estimatedDurationSec = (int) Math.round(distance / AVERAGE_WALK_SPEED_M_PER_SEC);

        return RouteCache.builder()
                .originPlaceId(originPlaceId)
                .destinationPlaceId(destinationPlaceId)
                .distanceM((int) Math.round(distance))
                .durationSec(estimatedDurationSec)
                .pathJson(toJson(List.of(origin, destination)))
                .source(RouteSource.HAVERSINE)
                .build();
    }

    private String toJson(List<RouteCoordinate> path) {
        return objectMapper.writeValueAsString(path);
    }
}
