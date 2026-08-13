package com.travelroute.backend.route.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Kakao 도보 경로 API 응답 스키마. 공식 문서로 확정된 스펙이 아니라
 * Kakao Mobility 길찾기 API(자동차)와 동일한 형태를 따른다고 가정하고 작성했다.
 * 실제 API 응답 형태가 다르면 이 DTO만 맞춰서 수정하면 된다 — 파싱 실패 시
 * KakaoWalkClient가 자동으로 Haversine 폴백으로 넘어가므로 서비스 동작에는 영향이 없다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoWalkResponse(
        List<Route> routes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            @JsonProperty("result_code") Integer resultCode,
            Summary summary,
            List<Section> sections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            Integer distance,
            Integer duration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Section(
            List<Road> roads
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Road(
            List<Double> vertexes
    ) {
    }
}
