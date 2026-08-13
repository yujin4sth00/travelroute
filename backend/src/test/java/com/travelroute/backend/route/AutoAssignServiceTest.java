package com.travelroute.backend.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.trip.Trip;
import com.travelroute.backend.trip.TripDay;
import com.travelroute.backend.trip.TripDayPlace;
import com.travelroute.backend.trip.TripDayPlaceRepository;
import com.travelroute.backend.trip.TripDayRepository;
import com.travelroute.backend.trip.TripNotFoundException;
import com.travelroute.backend.trip.TripRepository;
import com.travelroute.backend.trip.TripService;
import com.travelroute.backend.trip.dto.TripDetailResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AutoAssignServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripDayRepository tripDayRepository;

    @Mock
    private TripDayPlaceRepository tripDayPlaceRepository;

    @Mock
    private TripService tripService;

    @Spy
    private GeoClusterer geoClusterer = new GeoClusterer();

    @Spy
    private RouteOptimizer routeOptimizer = new RouteOptimizer();

    @InjectMocks
    private AutoAssignService autoAssignService;

    private Place place(Long id, double lat, double lng) {
        Place place = Place.builder().name("place-" + id).lat(lat).lng(lng).build();
        ReflectionTestUtils.setField(place, "id", id);
        return place;
    }

    private TripDay day(Trip trip, Long id, int dayNumber) {
        TripDay day = TripDay.builder().trip(trip).dayNumber(dayNumber).build();
        ReflectionTestUtils.setField(day, "id", id);
        return day;
    }

    private TripDayPlace tripDayPlace(Long id, TripDay day, Place place, int visitOrder, boolean locked) {
        TripDayPlace tdp = TripDayPlace.builder().tripDay(day).place(place).visitOrder(visitOrder).locked(locked).build();
        ReflectionTestUtils.setField(tdp, "id", id);
        return tdp;
    }

    @Test
    void autoAssign_throwsTripNotFoundException_whenTripMissing() {
        given(tripRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> autoAssignService.autoAssign(1L))
                .isInstanceOf(TripNotFoundException.class);
    }

    @Test
    void autoAssign_keepsLockedPlaceOnItsOriginalDay_andDistributesUnlockedByGeography() {
        Trip trip = Trip.builder().title("제주 3박4일").build();
        ReflectionTestUtils.setField(trip, "id", 1L);

        TripDay day1 = day(trip, 10L, 1);
        TripDay day2 = day(trip, 20L, 2);

        // 서울권 장소 3개, 부산권 장소 3개 (뚜렷하게 분리됨)
        Place seoul1 = place(101L, 37.5665, 126.9780);
        Place seoul2 = place(102L, 37.5700, 126.9820);
        Place seoul3 = place(103L, 37.5630, 126.9750);
        Place busan1 = place(201L, 35.1796, 129.0756);
        Place busan2 = place(202L, 35.1830, 129.0800);
        Place busan3 = place(203L, 35.1760, 129.0720);

        // 잠긴 장소: 지리적으로는 부산권이지만 Day1에 고정되어 있음 -> 재배치되면 안 됨
        TripDayPlace lockedOnDay1 = tripDayPlace(1L, day1, busan1, 1, true);

        TripDayPlace unlockedSeoul1 = tripDayPlace(2L, day1, seoul1, 2, false);
        TripDayPlace unlockedSeoul2 = tripDayPlace(3L, day2, seoul2, 1, false);
        TripDayPlace unlockedSeoul3 = tripDayPlace(4L, day2, seoul3, 2, false);
        TripDayPlace unlockedBusan2 = tripDayPlace(5L, day1, busan2, 3, false);
        TripDayPlace unlockedBusan3 = tripDayPlace(6L, day2, busan3, 3, false);

        List<TripDayPlace> allEntries = List.of(
                lockedOnDay1, unlockedSeoul1, unlockedBusan2, unlockedSeoul2, unlockedSeoul3, unlockedBusan3);

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(tripDayRepository.findByTripIdOrderByDayNumberAsc(1L)).willReturn(List.of(day1, day2));
        given(tripDayPlaceRepository.findAllByTripIdOrderByDayAndOrder(1L)).willReturn(allEntries);

        TripDetailResponse dummyResponse = new TripDetailResponse(1L, "제주 3박4일", null, null, List.of());
        given(tripService.getTripDetail(1L)).willReturn(dummyResponse);

        TripDetailResponse result = autoAssignService.autoAssign(1L);

        assertThat(result).isSameAs(dummyResponse);

        // 잠긴 장소는 그대로 Day1, 순서도 그대로
        assertThat(lockedOnDay1.getTripDay().getId()).isEqualTo(10L);
        assertThat(lockedOnDay1.getVisitOrder()).isEqualTo(1);
        assertThat(lockedOnDay1.isLocked()).isTrue();

        // 서울권 3개는 같은 날짜로, 부산권(잠기지 않은) 2개는 같은 날짜로 재배치되어야 함
        long seoulDayId = unlockedSeoul1.getTripDay().getId();
        assertThat(unlockedSeoul2.getTripDay().getId()).isEqualTo(seoulDayId);
        assertThat(unlockedSeoul3.getTripDay().getId()).isEqualTo(seoulDayId);

        long busanDayId = unlockedBusan2.getTripDay().getId();
        assertThat(unlockedBusan3.getTripDay().getId()).isEqualTo(busanDayId);

        assertThat(seoulDayId).isNotEqualTo(busanDayId);
    }

    @Test
    void autoAssign_returnsCurrentState_whenNothingIsUnlocked() {
        Trip trip = Trip.builder().title("여행").build();
        ReflectionTestUtils.setField(trip, "id", 1L);
        TripDay day1 = day(trip, 10L, 1);

        Place place = place(101L, 37.5, 127.0);
        TripDayPlace lockedOnly = tripDayPlace(1L, day1, place, 1, true);

        given(tripRepository.findById(1L)).willReturn(Optional.of(trip));
        given(tripDayRepository.findByTripIdOrderByDayNumberAsc(1L)).willReturn(List.of(day1));
        given(tripDayPlaceRepository.findAllByTripIdOrderByDayAndOrder(1L)).willReturn(List.of(lockedOnly));

        TripDetailResponse dummyResponse = new TripDetailResponse(1L, "여행", null, null, List.of());
        given(tripService.getTripDetail(1L)).willReturn(dummyResponse);

        TripDetailResponse result = autoAssignService.autoAssign(1L);

        assertThat(result).isSameAs(dummyResponse);
        assertThat(lockedOnly.getVisitOrder()).isEqualTo(1);
    }
}
