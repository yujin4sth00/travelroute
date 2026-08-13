package com.travelroute.backend.route;

public class MissingRoutePlacesException extends RuntimeException {

    public MissingRoutePlacesException(Long tripId, Long dayId) {
        super("동선 최적화를 위해서는 출발지와 도착지가 먼저 지정되어야 합니다. tripId=" + tripId + ", dayId=" + dayId);
    }
}
