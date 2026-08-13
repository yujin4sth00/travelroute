package com.travelroute.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.travelroute.backend.auth.dto.KakaoUserProfile;
import com.travelroute.backend.user.User;
import com.travelroute.backend.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KakaoOAuthClient kakaoOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithKakaoCode_createsNewUser_whenKakaoIdNotSeenBefore() {
        given(kakaoOAuthClient.exchangeCodeForAccessToken("auth-code")).willReturn("kakao-access-token");
        given(kakaoOAuthClient.fetchUserProfile("kakao-access-token"))
                .willReturn(new KakaoUserProfile("12345", "홍길동", "http://img"));
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });
        given(jwtTokenProvider.generateToken(1L)).willReturn("issued-jwt");

        String token = authService.loginWithKakaoCode("auth-code");

        assertThat(token).isEqualTo("issued-jwt");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void loginWithKakaoCode_updatesExistingUserProfile_whenKakaoIdAlreadyExists() {
        User existing = User.builder().kakaoId("12345").nickname("old-name").profileImage("old-img").build();
        ReflectionTestUtils.setField(existing, "id", 7L);

        given(kakaoOAuthClient.exchangeCodeForAccessToken("auth-code")).willReturn("kakao-access-token");
        given(kakaoOAuthClient.fetchUserProfile("kakao-access-token"))
                .willReturn(new KakaoUserProfile("12345", "새이름", "http://new-img"));
        given(userRepository.findByKakaoId("12345")).willReturn(Optional.of(existing));
        given(jwtTokenProvider.generateToken(7L)).willReturn("issued-jwt");

        String token = authService.loginWithKakaoCode("auth-code");

        assertThat(token).isEqualTo("issued-jwt");
        assertThat(existing.getNickname()).isEqualTo("새이름");
        assertThat(existing.getProfileImage()).isEqualTo("http://new-img");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void buildKakaoAuthorizeUrl_delegatesToKakaoOAuthClient() {
        given(kakaoOAuthClient.buildAuthorizeUrl()).willReturn("https://kauth.kakao.com/oauth/authorize?client_id=abc");

        String url = authService.buildKakaoAuthorizeUrl();

        assertThat(url).isEqualTo("https://kauth.kakao.com/oauth/authorize?client_id=abc");
    }
}
