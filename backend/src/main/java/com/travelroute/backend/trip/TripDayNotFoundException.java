package com.travelroute.backend.trip;

public class TripDayNotFoundException extends RuntimeException {

    public TripDayNotFoundException(Long tripId, Long dayId) {
        super("여행 일차를 찾을 수 없습니다. tripId=" + tripId + ", dayId=" + dayId);
    }
}
