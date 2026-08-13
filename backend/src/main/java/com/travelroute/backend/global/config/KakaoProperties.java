package com.travelroute.backend.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        String restApiKey,
        String localApiBaseUrl,
        String walkApiBaseUrl
) {
}
