package com.travelroute.backend.trip;

public class TripDayPlaceNotFoundException extends RuntimeException {

    public TripDayPlaceNotFoundException(Long id, Long dayId) {
        super("일차에 배치된 장소를 찾을 수 없습니다. id=" + id + ", dayId=" + dayId);
    }
}
