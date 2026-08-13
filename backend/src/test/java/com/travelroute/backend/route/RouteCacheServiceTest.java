package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.route.dto.RouteSegmentResponse;
import com.travelroute.backend.route.kakao.KakaoWalkClient;
import com.travelroute.backend.route.kakao.WalkRouteResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RouteCacheServiceTest {

    @Mock
    private RouteCacheRepository routeCacheRepository;

    @Mock
    private KakaoWalkClient kakaoWalkClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RouteCacheService routeCacheService;

    private Place place(Long id, double lat, double lng) {
        Place place = Place.builder().name("place-" + id).lat(lat).lng(lng).build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    @Test
    void getOrFetchSegment_returnsCachedSegment_withoutCallingKakaoWalkClient() {
        Place origin = place(1L, 37.50, 127.00);
        Place destination = place(2L, 37.51, 127.01);

        RouteCache cache = RouteCache.builder()
                .originPlaceId(1L).destinationPlaceId(2L)
                .distanceM(500).durationSec(400)
                .pathJson("[]").source(RouteSource.KAKAO_WALK)
                .build();

        given(routeCacheRepository.findByOriginPlaceIdAndDestinationPlaceId(1L, 2L))
                .willReturn(Optional.of(cache));

        RouteSegmentResponse response = routeCacheService.getOrFetchSegment(origin, destination);

        assertThat(response.distanceM()).isEqualTo(500);
        assertThat(response.source()).isEqualTo(RouteSource.KAKAO_WALK);
        verify(kakaoWalkClient, never()).fetchWalkingRoute(any(), any());
        verify(routeCacheRepository, never()).save(any());
    }

    @Test
    void getOrFetchSegment_savesKakaoWalkResult_whenApiSucceeds() {
        Place origin = place(1L, 37.50, 127.00);
        Place destination = place(2L, 37.51, 127.01);

        given(routeCacheRepository.findByOriginPlaceIdAndDestinationPlaceId(1L, 2L))
                .willReturn(Optional.empty());
        given(kakaoWalkClient.fetchWalkingRoute(any(), any()))
                .willReturn(Optional.of(new WalkRouteResult(620, 480,
                        List.of(new RouteCoordinate(37.50, 127.00), new RouteCoordinate(37.51, 127.01)))));

        RouteSegmentResponse response = routeCacheService.getOrFetchSegment(origin, destination);

        assertThat(response.distanceM()).isEqualTo(620);
        assertThat(response.durationSec()).isEqualTo(480);
        assertThat(response.source()).isEqualTo(RouteSource.KAKAO_WALK);

        ArgumentCaptor<RouteCache> captor = ArgumentCaptor.captor();
        verify(routeCacheRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(RouteSource.KAKAO_WALK);
        assertThat(captor.getValue().getOriginPlaceId()).isEqualTo(1L);
        assertThat(captor.getValue().getDestinationPlaceId()).isEqualTo(2L);
    }

    @Test
    void getOrFetchSegment_fallsBackToHaversine_whenKakaoApiUnavailable() {
        Place origin = place(1L, 37.5665, 126.9780);
        Place destination = place(2L, 37.5765, 126.9880);

        given(routeCacheRepository.findByOriginPlaceIdAndDestinationPlaceId(1L, 2L))
                .willReturn(Optional.empty());
        given(kakaoWalkClient.fetchWalkingRoute(any(), any())).willReturn(Optional.empty());

        RouteSegmentResponse response = routeCacheService.getOrFetchSegment(origin, destination);

        double expectedDistance = HaversineCalculator.distanceMeters(37.5665, 126.9780, 37.5765, 126.9880);

        assertThat(response.source()).isEqualTo(RouteSource.HAVERSINE);
        assertThat(response.distanceM()).isEqualTo((int) Math.round(expectedDistance));
        assertThat(response.durationSec()).isGreaterThan(0);

        ArgumentCaptor<RouteCache> captor = ArgumentCaptor.captor();
        verify(routeCacheRepository).save(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo(RouteSource.HAVERSINE);
    }
}
