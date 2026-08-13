package com.travelroute.backend.trip;

public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(Long id) {
        super("여행을 찾을 수 없습니다. id=" + id);
    }
}
