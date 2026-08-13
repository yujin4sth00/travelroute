package com.travelroute.backend.auth;

import com.travelroute.backend.auth.dto.KakaoTokenResponse;
import com.travelroute.backend.auth.dto.KakaoUserProfile;
import com.travelroute.backend.auth.dto.KakaoUserResponse;
import com.travelroute.backend.global.config.KakaoProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Kakao 로그인(OAuth2) 연동. 인가 코드를 액세스 토큰으로 교환하고, 그 토큰으로 사용자 프로필을 조회한다.
 * Kakao 지도(즐겨찾기) 데이터를 가져오는 API는 별도로 존재하지 않으므로, 여기서는 로그인/식별 용도로만 사용한다.
 */
@Component
public class KakaoOAuthClient {

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_ME_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;
    private final KakaoOAuthProperties kakaoOAuthProperties;

    public KakaoOAuthClient(RestClient.Builder restClientBuilder, KakaoProperties kakaoProperties,
                             KakaoOAuthProperties kakaoOAuthProperties) {
        this.restClient = restClientBuilder.build();
        this.kakaoProperties = kakaoProperties;
        this.kakaoOAuthProperties = kakaoOAuthProperties;
    }

    public String buildAuthorizeUrl() {
        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("client_id", kakaoProperties.restApiKey())
                .queryParam("redirect_uri", kakaoOAuthProperties.redirectUri())
                .queryParam("response_type", "code")
                .build()
                .toUriString();
    }

    public String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoProperties.restApiKey());
        form.add("redirect_uri", kakaoOAuthProperties.redirectUri());
        form.add("code", code);

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KakaoOAuthException("Kakao 토큰 발급에 실패했습니다.");
            }
            return response.accessToken();
        } catch (RestClientException e) {
            throw new KakaoOAuthException("Kakao 토큰 발급 요청이 실패했습니다: " + e.getMessage());
        }
    }

    public KakaoUserProfile fetchUserProfile(String kakaoAccessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(USER_ME_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.id() == null) {
                throw new KakaoOAuthException("Kakao 사용자 정보 조회에 실패했습니다.");
            }

            String nickname = null;
            String profileImage = null;
            if (response.kakaoAccount() != null && response.kakaoAccount().profile() != null) {
                nickname = response.kakaoAccount().profile().nickname();
                profileImage = response.kakaoAccount().profile().profileImageUrl();
            }

            return new KakaoUserProfile(String.valueOf(response.id()), nickname, profileImage);
        } catch (RestClientException e) {
            throw new KakaoOAuthException("Kakao 사용자 정보 조회 요청이 실패했습니다: " + e.getMessage());
        }
    }
}
