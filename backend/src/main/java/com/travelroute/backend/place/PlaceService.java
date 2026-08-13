package com.travelroute.backend.place;

import com.travelroute.backend.place.dto.PlaceCreateRequest;
import com.travelroute.backend.place.dto.PlaceResponse;
import com.travelroute.backend.place.dto.PlaceSearchResult;
import com.travelroute.backend.place.kakao.KakaoLocalClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final KakaoLocalClient kakaoLocalClient;

    public List<PlaceSearchResult> search(String query) {
        return kakaoLocalClient.searchByKeyword(query);
    }

    @Transactional
    public PlaceResponse save(PlaceCreateRequest request) {
        Place place = Place.builder()
                .name(request.name())
                .address(request.address())
                .lat(request.lat())
                .lng(request.lng())
                .category(request.category())
                .memo(request.memo())
                .kakaoPlaceId(request.kakaoPlaceId())
                .build();

        return PlaceResponse.from(placeRepository.save(place));
    }

    public List<PlaceResponse> findAll() {
        return placeRepository.findAll().stream()
                .map(PlaceResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        if (!placeRepository.existsById(id)) {
            throw new PlaceNotFoundException(id);
        }
        placeRepository.deleteById(id);
    }
}
