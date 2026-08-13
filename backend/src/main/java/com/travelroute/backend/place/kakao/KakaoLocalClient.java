package com.travelroute.backend.place.kakao;

import com.travelroute.backend.global.config.KakaoProperties;
import com.travelroute.backend.global.exception.KakaoApiException;
import com.travelroute.backend.place.dto.PlaceSearchResult;
import com.travelroute.backend.place.kakao.dto.KakaoLocalSearchResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KakaoLocalClient {

    private static final String KEYWORD_SEARCH_PATH = "/search/keyword.json";

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    public KakaoLocalClient(RestClient.Builder restClientBuilder, KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;
        this.restClient = restClientBuilder
                .baseUrl(kakaoProperties.localApiBaseUrl())
                .build();
    }

    public List<PlaceSearchResult> searchByKeyword(String query) {
        if (kakaoProperties.restApiKey() == null || kakaoProperties.restApiKey().isBlank()) {
            throw new KakaoApiException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
        }

        KakaoLocalSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(KEYWORD_SEARCH_PATH)
                            .queryParam("query", query)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.restApiKey())
                    .retrieve()
                    .body(KakaoLocalSearchResponse.class);
        } catch (RestClientResponseException e) {
            throw new KakaoApiException(e.getStatusCode(), "Kakao Local API 호출에 실패했습니다: " + e.getMessage());
        }

        if (response == null || response.documents() == null) {
            return List.of();
        }

        return response.documents().stream()
                .map(this::toPlaceSearchResult)
                .toList();
    }

    private PlaceSearchResult toPlaceSearchResult(KakaoLocalSearchResponse.Document document) {
        String address = (document.roadAddressName() != null && !document.roadAddressName().isBlank())
                ? document.roadAddressName()
                : document.addressName();

        return new PlaceSearchResult(
                document.id(),
                document.placeName(),
                address,
                Double.parseDouble(document.y()),
                Double.parseDouble(document.x()),
                document.categoryName()
        );
    }
}
