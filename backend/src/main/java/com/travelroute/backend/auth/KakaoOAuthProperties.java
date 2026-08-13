package com.travelroute.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.oauth")
public record KakaoOAuthProperties(
        String redirectUri
) {
}
