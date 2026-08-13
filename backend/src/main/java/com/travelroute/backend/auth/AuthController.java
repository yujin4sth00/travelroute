package com.travelroute.backend.auth;

import com.travelroute.backend.global.config.AppProperties;
import com.travelroute.backend.user.User;
import com.travelroute.backend.user.UserRepository;
import com.travelroute.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;
    private final UserRepository userRepository;

    @GetMapping("/kakao/login")
    public ResponseEntity<Void> loginRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, authService.buildKakaoAuthorizeUrl())
                .build();
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code) {
        String token = authService.loginWithKakaoCode(code);

        String redirectUrl = UriComponentsBuilder.fromUriString(appProperties.frontendBaseUrl())
                .path("/auth/callback")
                .queryParam("token", token)
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("인증된 사용자를 찾을 수 없습니다. id=" + userId));
        return UserResponse.from(user);
    }
}
