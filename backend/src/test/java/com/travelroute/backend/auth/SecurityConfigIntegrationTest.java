package com.travelroute.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * JWT 필터 + SecurityFilterChain이 실제로 요청을 막고 통과시키는지 검증한다.
 * 실제 Kakao 앱(client id/secret, redirect URI 화이트리스트)이 없어 인가 코드 교환까지는
 * 여기서 검증할 수 없다 — 그 부분은 실제 Kakao 앱을 등록한 뒤 브라우저로 확인해야 한다.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class SecurityConfigIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @LocalServerPort
    private int port;

    @Test
    void protectedEndpoint_returns401_withoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/places", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_returns401_withGarbageToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-real-jwt");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/places", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_succeeds_withValidToken() {
        String token = jwtTokenProvider.generateToken(1L);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/places", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void kakaoLoginEndpoint_isPubliclyAccessible_withoutToken() throws Exception {
        // TestRestTemplate은 리다이렉트를 자동으로 따라가서 실제 Kakao 서버까지 나가버리므로,
        // 여기서는 리다이렉트를 따라가지 않는 순수 JDK HttpClient로 302 응답 자체만 확인한다.
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/kakao/login"))
                .GET()
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElse("")).contains("kauth.kakao.com");
    }
}
