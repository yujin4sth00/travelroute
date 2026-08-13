package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.route.dto.DayRouteResponse;
import com.travelroute.backend.route.dto.RouteSegmentResponse;
import com.travelroute.backend.trip.Trip;
import com.travelroute.backend.trip.TripAccessGuard;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayNotFoundException;
import com.travelroute.backend.trip.TripDayPlace;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DayRouteServiceTest {

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private TripDayPlaceRepository tripDayPlaceRepository;

    @Mock
    private RouteCacheService routeCacheService;

    @Mock
    private TripAccessGuard tripAccessGuard;

    @InjectMocks
    private DayRouteService dayRouteService;

    private Place place(Long id, double lat, double lng) {
        Place place = Place.builder().name("place-" + id).lat(lat).lng(lng).build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private Trip ownedTrip() {
        Trip trip = Trip.builder().userId(1L).title("여행").build();
        ReflectionTestUtils.setField(trip, "id", 1L);
        return trip;
    }

    @Test
    void getDayRoute_throwsTripDayNotFoundException_whenDayNotOwnedByTrip() {
        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(ownedTrip());
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> dayRouteService.getDayRoute(1L, 10L))
                .isInstanceOf(TripDayNotFoundException.class);
    }

    @Test
    void getDayRoute_throwsMissingRoutePlacesException_whenStartOrEndNotAssigned() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.of(day));

        assertThatThrownBy(() -> dayRouteService.getDayRoute(1L, 10L))
                .isInstanceOf(MissingRoutePlacesException.class);
    }

    @Test
    void getDayRoute_buildsFullSequenceAndSumsSegmentTotals() {
        Place start = place(1L, 0.0, 0.0);
        Place end = place(4L, 3.0, 3.0);
        Place p1 = place(2L, 1.0, 1.0);
        Place p2 = place(3L, 2.0, 2.0);

        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);
        day.assignPlaces(start, end);

        TripDayPlace tdp1 = TripDayPlace.builder().tripDay(day).place(p1).visitOrder(1).locked(false).build();
        TripDayPlace tdp2 = TripDayPlace.builder().tripDay(day).place(p2).visitOrder(2).locked(false).build();

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(Optional.of(day));
        given(tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(10L))
                .willReturn(List.of(tdp1, tdp2));

        given(routeCacheService.getOrFetchSegment(start, p1))
                .willReturn(new RouteSegmentResponse(1L, 2L, 100, 90, "[]", RouteSource.HAVERSINE));
        given(routeCacheService.getOrFetchSegment(p1, p2))
                .willReturn(new RouteSegmentResponse(2L, 3L, 150, 130, "[]", RouteSource.HAVERSINE));
        given(routeCacheService.getOrFetchSegment(p2, end))
                .willReturn(new RouteSegmentResponse(3L, 4L, 200, 180, "[]", RouteSource.HAVERSINE));

        DayRouteResponse response = dayRouteService.getDayRoute(1L, 10L);

        assertThat(response.segments()).hasSize(3);
        assertThat(response.totalDistanceM()).isEqualTo(450);
        assertThat(response.totalDurationSec()).isEqualTo(400);
    }
}
