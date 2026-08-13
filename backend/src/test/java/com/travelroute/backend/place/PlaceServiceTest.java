package com.travelroute.backend.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelroute.backend.auth.CurrentUserProvider;
import com.travelroute.backend.place.dto.PlaceCreateRequest;
import com.travelroute.backend.place.dto.PlaceResponse;
import com.travelroute.backend.place.dto.PlaceSearchResult;
import com.travelroute.backend.place.kakao.KakaoLocalClient;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private KakaoLocalClient kakaoLocalClient;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private PlaceService placeService;

    @Test
    void search_delegatesToKakaoLocalClient() {
        String query = "강남역 카페";
        List<PlaceSearchResult> expected = List.of(
                new PlaceSearchResult("111", "카페 A", "서울 강남구", 37.123, 127.456, "카페")
        );
        given(kakaoLocalClient.searchByKeyword(query)).willReturn(expected);

        List<PlaceSearchResult> result = placeService.search(query);

        assertThat(result).isEqualTo(expected);
        verify(kakaoLocalClient).searchByKeyword(query);
    }

    @Test
    void save_persistsPlaceAndReturnsResponse() {
        given(currentUserProvider.getUserId()).willReturn(1L);

        PlaceCreateRequest request = new PlaceCreateRequest(
                "카페 A", "서울 강남구", 37.123, 127.456, "카페", "메모", "111");

        Place savedPlace = Place.builder()
                .userId(1L)
                .name(request.name())
                .address(request.address())
                .lat(request.lat())
                .lng(request.lng())
                .category(request.category())
                .memo(request.memo())
                .kakaoPlaceId(request.kakaoPlaceId())
                .build();
        ReflectionTestUtils.setField(savedPlace, "id", 1L);
        ReflectionTestUtils.setField(savedPlace, "createdAt", LocalDateTime.now());

        given(placeRepository.save(any(Place.class))).willReturn(savedPlace);

        PlaceResponse response = placeService.save(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("카페 A");
        assertThat(response.kakaoPlaceId()).isEqualTo("111");
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    void delete_removesPlace_whenExists() {
        given(currentUserProvider.getUserId()).willReturn(1L);

        Place place = Place.builder().userId(1L).name("카페 A").lat(37.123).lng(127.456).build();
        ReflectionTestUtils.setField(place, "id", 1L);

        given(placeRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(place));

        placeService.delete(1L);

        verify(placeRepository).delete(place);
    }

    @Test
    void delete_throwsPlaceNotFoundException_whenNotExists() {
        given(currentUserProvider.getUserId()).willReturn(1L);
        given(placeRepository.findByIdAndUserId(999L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.delete(999L))
                .isInstanceOf(PlaceNotFoundException.class);

        verify(placeRepository, never()).delete(any(Place.class));
    }
}
