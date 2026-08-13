package com.travelroute.backend.global.exception;

import com.travelroute.backend.place.PlaceNotFoundException;
import com.travelroute.backend.route.MissingRoutePlacesException;
import com.travelroute.backend.trip.InvalidReorderRequestException;
import com.travelroute.backend.trip.InvalidTripPeriodException;
import com.travelroute.backend.trip.TripDayNotFoundException;
import com.travelroute.backend.trip.TripDayPlaceNotFoundException;
import com.travelroute.backend.trip.TripNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ErrorResponse> handleKakaoApiException(KakaoApiException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({
            PlaceNotFoundException.class,
            TripNotFoundException.class,
            TripDayNotFoundException.class,
            TripDayPlaceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler({
            InvalidTripPeriodException.class,
            InvalidReorderRequestException.class,
            MissingRoutePlacesException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }
}
