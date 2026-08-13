package com.travelroute.backend.trip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelroute.backend.auth.CurrentUserProvider;
import com.travelroute.backend.place.Place;
import com.travelroute.backend.place.PlaceRepository;
import com.travelroute.backend.trip.dto.ReorderRequest;
import com.travelroute.backend.trip.dto.TripCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceResponse;
import com.travelroute.backend.trip.dto.TripResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private TripDayPlaceRepository tripDayPlaceRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TripAccessGuard tripAccessGuard;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private TripService tripService;

    private Trip ownedTrip() {
        Trip trip = Trip.builder().userId(1L).title("여행").build();
        ReflectionTestUtils.setField(trip, "id", 1L);
        return trip;
    }

    @Test
    void createTrip_generatesOneTripDayPerCalendarDay() {
        given(currentUserProvider.getUserId()).willReturn(1L);

        TripCreateRequest request = new TripCreateRequest(
                "제주 여행", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3));

        given(tripRepository.save(any(Trip.class))).willAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            ReflectionTestUtils.setField(trip, "id", 1L);
            return trip;
        });

        TripResponse response = tripService.createTrip(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("제주 여행");

        ArgumentCaptor<List<TripDay>> daysCaptor = ArgumentCaptor.captor();
        verify(tripDayRepository).saveAll(daysCaptor.capture());

        List<TripDay> days = daysCaptor.getValue();
        assertThat(days).hasSize(3);
        assertThat(days.get(0).getDayNumber()).isEqualTo(1);
        assertThat(days.get(0).getDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(days.get(2).getDayNumber()).isEqualTo(3);
        assertThat(days.get(2).getDate()).isEqualTo(LocalDate.of(2026, 9, 3));
    }

    @Test
    void createTrip_throwsInvalidTripPeriodException_whenEndBeforeStart() {
        TripCreateRequest request = new TripCreateRequest(
                "잘못된 여행", LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> tripService.createTrip(request))
                .isInstanceOf(InvalidTripPeriodException.class);

        verify(tripRepository, never()).save(any(Trip.class));
    }

    @Test
    void addPlaceToDay_assignsNextVisitOrderAfterExistingMax() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        Place place = Place.builder().userId(1L).name("카페").lat(37.0).lng(127.0).build();
        ReflectionTestUtils.setField(place, "id", 5L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(java.util.Optional.of(day));
        given(currentUserProvider.getUserId()).willReturn(1L);
        given(placeRepository.findByIdAndUserId(5L, 1L)).willReturn(java.util.Optional.of(place));
        given(tripDayPlaceRepository.findMaxVisitOrder(10L)).willReturn(2);
        given(tripDayPlaceRepository.save(any(TripDayPlace.class))).willAnswer(invocation -> invocation.getArgument(0));

        TripDayPlaceCreateRequest request = new TripDayPlaceCreateRequest(5L);
        TripDayPlaceResponse response = tripService.addPlaceToDay(1L, 10L, request);

        assertThat(response.visitOrder()).isEqualTo(3);
        assertThat(response.locked()).isFalse();
        assertThat(response.placeId()).isEqualTo(5L);
    }

    @Test
    void reorderPlaces_locksAndReassignsVisitOrderInGivenSequence() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        Place placeA = Place.builder().userId(1L).name("A").lat(1.0).lng(1.0).build();
        Place placeB = Place.builder().userId(1L).name("B").lat(2.0).lng(2.0).build();

        TripDayPlace tdp1 = TripDayPlace.builder().tripDay(day).place(placeA).visitOrder(1).locked(false).build();
        ReflectionTestUtils.setField(tdp1, "id", 100L);
        TripDayPlace tdp2 = TripDayPlace.builder().tripDay(day).place(placeB).visitOrder(2).locked(false).build();
        ReflectionTestUtils.setField(tdp2, "id", 200L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(java.util.Optional.of(day));
        given(tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(10L)).willReturn(List.of(tdp1, tdp2));

        ReorderRequest request = new ReorderRequest(List.of(200L, 100L));
        List<TripDayPlaceResponse> result = tripService.reorderPlaces(1L, 10L, request);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(200L);
        assertThat(result.get(0).visitOrder()).isEqualTo(1);
        assertThat(result.get(0).locked()).isTrue();
        assertThat(result.get(1).id()).isEqualTo(100L);
        assertThat(result.get(1).visitOrder()).isEqualTo(2);
        assertThat(result.get(1).locked()).isTrue();
    }

    @Test
    void reorderPlaces_throwsInvalidReorderRequestException_whenIdSetMismatches() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        Place placeA = Place.builder().userId(1L).name("A").lat(1.0).lng(1.0).build();
        TripDayPlace tdp1 = TripDayPlace.builder().tripDay(day).place(placeA).visitOrder(1).locked(false).build();
        ReflectionTestUtils.setField(tdp1, "id", 100L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(java.util.Optional.of(day));
        given(tripDayPlaceRepository.findByTripDayIdOrderByVisitOrderAsc(10L)).willReturn(List.of(tdp1));

        ReorderRequest request = new ReorderRequest(List.of(999L));

        assertThatThrownBy(() -> tripService.reorderPlaces(1L, 10L, request))
                .isInstanceOf(InvalidReorderRequestException.class);
    }

    @Test
    void removePlaceFromDay_throwsTripDayPlaceNotFoundException_whenNotBelongingToDay() {
        Trip trip = ownedTrip();
        TripDay day = TripDay.builder().trip(trip).dayNumber(1).build();
        ReflectionTestUtils.setField(day, "id", 10L);

        given(tripAccessGuard.requireOwnedTrip(1L)).willReturn(trip);
        given(tripDayRepository.findByIdAndTripId(10L, 1L)).willReturn(java.util.Optional.of(day));
        given(tripDayPlaceRepository.findByIdAndTripDayId(999L, 10L)).willReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> tripService.removePlaceFromDay(1L, 10L, 999L))
                .isInstanceOf(TripDayPlaceNotFoundException.class);
    }
}
