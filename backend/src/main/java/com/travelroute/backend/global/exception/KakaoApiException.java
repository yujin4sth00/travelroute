package com.travelroute.backend.global.exception;

import org.springframework.http.HttpStatusCode;

public class KakaoApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public KakaoApiException(String message) {
        this(HttpStatusCode.valueOf(502), message);
    }

    public KakaoApiException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
