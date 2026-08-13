package com.travelroute.backend.route.kakao;

import com.travelroute.backend.global.config.KakaoProperties;
import com.travelroute.backend.route.RouteCoordinate;
import com.travelroute.backend.route.kakao.dto.KakaoWalkResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Kakao 도보 경로 API 프록시. 실패하거나 응답을 해석할 수 없으면 빈 Optional을 반환해서
 * 호출 측(RouteCacheService)이 Haversine 직선거리로 폴백하도록 한다.
 */
@Slf4j
@Component
public class KakaoWalkClient {

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    public KakaoWalkClient(RestClient.Builder restClientBuilder, KakaoProperties kakaoProperties) {
        this.kakaoProperties = kakaoProperties;
        this.restClient = restClientBuilder
                .baseUrl(kakaoProperties.walkApiBaseUrl())
                .build();
    }

    public Optional<WalkRouteResult> fetchWalkingRoute(RouteCoordinate origin, RouteCoordinate destination) {
        if (kakaoProperties.restApiKey() == null || kakaoProperties.restApiKey().isBlank()) {
            return Optional.empty();
        }

        try {
            KakaoWalkResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("origin", origin.lng() + "," + origin.lat())
                            .queryParam("destination", destination.lng() + "," + destination.lat())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoProperties.restApiKey())
                    .retrieve()
                    .body(KakaoWalkResponse.class);

            return parse(response);
        } catch (RestClientException e) {
            log.warn("Kakao 도보 경로 API 호출 실패, Haversine으로 폴백합니다: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<WalkRouteResult> parse(KakaoWalkResponse response) {
        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return Optional.empty();
        }

        KakaoWalkResponse.Route route = response.routes().get(0);
        if (route.resultCode() == null || route.resultCode() != 0 || route.summary() == null) {
            return Optional.empty();
        }

        List<RouteCoordinate> path = new ArrayList<>();
        if (route.sections() != null) {
            for (KakaoWalkResponse.Section section : route.sections()) {
                if (section.roads() == null) {
                    continue;
                }
                for (KakaoWalkResponse.Road road : section.roads()) {
                    appendVertexes(path, road.vertexes());
                }
            }
        }

        return Optional.of(new WalkRouteResult(route.summary().distance(), route.summary().duration(), path));
    }

    private void appendVertexes(List<RouteCoordinate> path, List<Double> vertexes) {
        if (vertexes == null) {
            return;
        }
        for (int i = 0; i + 1 < vertexes.size(); i += 2) {
            double lng = vertexes.get(i);
            double lat = vertexes.get(i + 1);
            path.add(new RouteCoordinate(lat, lng));
        }
    }
}
