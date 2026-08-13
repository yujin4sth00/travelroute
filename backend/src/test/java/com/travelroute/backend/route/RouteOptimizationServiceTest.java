package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.trip.Trip;
import com.travelroute.backend.trip.TripAccessGuard;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayNotFoundException;
import com.travelroute.backend.trip.TripDayPlace;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import com.travelroute.backend.trip.dto.TripDayPlaceResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RouteOptimizationServiceTest {

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private TripDayPlaceRepository tripDayPlaceRepository;

    @Mock
    private RouteOptimizer routeOptimizer;

    @Mock
    private TripAccessGuard tripAccessGuard;

    @InjectMocks
    private RouteOptimizationService routeOptimizationService;

    private Trip ownedTrip() {
        Trip trip = Trip.builder().userId(1L).title("여행").build();
        ReflectionTestUtils.setField(trip, "id", 1L);
        return trip;
    }

    @Test
    void optimizeDay_throwsTripDayNotFoundException_whenDayNotOwnedByTrip() {
        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(ownedTrip());
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> routeOptimizationService.optimizeDay(1L, 10L))
                .isInstanceOf(TripDayNotFoundException.class);
    }

    @Test
    void optimizeDay_throwsMissingRoutePlacesException_whenStartOrEndNotAssigned() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.of(day));

        assertThatThrownBy(() -> routeOptimizationService.optimizeDay(1L, 10L))
                .isInstanceOf(MissingRoutePlacesException.class);
    }

    @Test
    void optimizeDay_keepsLockedPlacePosition_andFillsUnlockedSlotsWithOptimizedOrder() {
        Place start = Place.builder().name("시작").lat(0.0).lng(0.0).build();
        Place end = Place.builder().name("도착").lat(10.0).lng(10.0).build();

        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);
        day.assignPlaces(start, end);

        Place p1 = Place.builder().name("P1").lat(1.0).lng(1.0).build();
        Place p2 = Place.builder().name("P2").lat(2.0).lng(2.0).build();
        Place p3 = Place.builder().name("P3").lat(3.0).lng(3.0).build();

        TripDayPlace tdp1 = TripDayPlace.builder().tripDay(day).place(p1).visitOrder(1).locked(false).build();
        ReflectionTestUtils.setField(tdp1, "id", 101L);
        TripDayPlace tdp2 = TripDayPlace.builder().tripDay(day).place(p2).visitOrder(2).locked(true).build();
        ReflectionTestUtils.setField(tdp2, "id", 102L);
        TripDayPlace tdp3 = TripDayPlace.builder().tripDay(day).place(p3).visitOrder(3).locked(false).build();
        ReflectionTestUtils.setField(tdp3, "id", 103L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.of(day));
        given(tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(10L))
                .willReturn(List.of(tdp1, tdp2, tdp3));

        // 최적화 결과를 입력 순서의 역순(tdp3 -> tdp1)으로 만드는 스텁: 잠기지 않은 슬롯에만 반영되는지 검증
        given(routeOptimizer.optimize(any(), any(), any())).willAnswer(invocation -> {
            List<RoutePoint> waypoints = invocation.getArgument(2);
            List<RoutePoint> reversed = new ArrayList<>(waypoints);
            Collections.reverse(reversed);
            return reversed;
        });

        List<TripDayPlaceResponse> result = routeOptimizationService.optimizeDay(1L, 10L);

        assertThat(tdp2.getVisitOrder()).isEqualTo(2);
        assertThat(tdp2.isLocked()).isTrue();

        assertThat(tdp3.getVisitOrder()).isEqualTo(1);
        assertThat(tdp3.isLocked()).isFalse();
        assertThat(tdp1.getVisitOrder()).isEqualTo(3);
        assertThat(tdp1.isLocked()).isFalse();

        assertThat(result).extracting(TripDayPlaceResponse::id)
                .containsExactly(103L, 102L, 101L);
    }

    @Test
    void optimizeDay_skipsOptimizerCall_whenAllPlacesAreLocked() {
        Place start = Place.builder().name("시작").lat(0.0).lng(0.0).build();
        Place end = Place.builder().name("도착").lat(10.0).lng(10.0).build();

        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);
        day.assignPlaces(start, end);

        Place p1 = Place.builder().name("P1").lat(1.0).lng(1.0).build();
        TripDayPlace tdp1 = TripDayPlace.builder().tripDay(day).place(p1).visitOrder(1).locked(true).build();
        ReflectionTestUtils.setField(tdp1, "id", 101L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.of(day));
        given(tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(10L)).willReturn(List.of(tdp1));

        List<TripDayPlaceResponse> result = routeOptimizationService.optimizeDay(1L, 10L);

        assertThat(result).hasSize(1);
        assertThat(tdp1.getVisitOrder()).isEqualTo(1);
    }
}
