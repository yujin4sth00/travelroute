package com.travelroute.backend.trip;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelroute.backend.place.Place;
import com.travelroute.backend.place.PlaceRepository;
import com.travelroute.backend.trip.dto.TripCreateRequest;
import com.travelroute.backend.trip.dto.TripDayPlaceCreateRequest;
import com.travelroute.backend.trip.dto.TripDetailResponse;
import com.travelroute.backend.trip.dto.TripResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 실제 H2 DB와 실제 트랜잭션 경계를 사용하는 통합 테스트.
 * 이 테스트에는 일부러 @Transactional을 걸지 않는다 — 각 서비스 호출이 운영 환경처럼
 * 별도 트랜잭션으로 커밋/종료되어야, TripDayPlace -> Place 지연 로딩이 트랜잭션 밖에서
 * 실패하지 않는지(getTripDetail에 @Transactional이 빠지는 회귀) 검증할 수 있다.
 */
@SpringBootTest
class TripServiceIntegrationTest {

    private static final Long TEST_USER_ID = 1L;

    @Autowired
    private TripService tripService;

    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void authenticateAsTestUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTripDetail_doesNotFailOnLazyLoadedPlace_outsideOriginalTransaction() {
        Place place = placeRepository.save(
                Place.builder().userId(TEST_USER_ID).name("통합테스트장소").lat(33.5).lng(126.5).build());

        TripResponse trip = tripService.createTrip(
                new TripCreateRequest("통합테스트여행", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)));

        Long dayId = tripService.getTripDetail(trip.id()).days().get(0).id();

        tripService.addPlaceToDay(trip.id(), dayId, new TripDayPlaceCreateRequest(place.getId()));

        TripDetailResponse detail = tripService.getTripDetail(trip.id());

        assertThat(detail.days()).hasSize(1);
        assertThat(detail.days().get(0).places()).hasSize(1);
        assertThat(detail.days().get(0).places().get(0).placeName()).isEqualTo("통합테스트장소");
    }
}
