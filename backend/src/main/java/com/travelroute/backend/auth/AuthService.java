package com.travelroute.backend.auth;

import com.travelroute.backend.auth.dto.KakaoUserProfile;
import com.travelroute.backend.user.User;
import com.travelroute.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public String buildKakaoAuthorizeUrl() {
        return kakaoOAuthClient.buildAuthorizeUrl();
    }

    @Transactional
    public String loginWithKakaoCode(String code) {
        String kakaoAccessToken = kakaoOAuthClient.exchangeCodeForAccessToken(code);
        KakaoUserProfile profile = kakaoOAuthClient.fetchUserProfile(kakaoAccessToken);

        User user = userRepository.findByKakaoId(profile.kakaoId())
                .map(existing -> {
                    existing.updateProfile(profile.nickname(), profile.profileImage());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .kakaoId(profile.kakaoId())
                        .nickname(profile.nickname())
                        .profileImage(profile.profileImage())
                        .build()));

        return jwtTokenProvider.generateToken(user.getId());
    }
}
