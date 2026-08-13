package com.travelroute.backend.place;

public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException(Long id) {
        super("장소를 찾을 수 없습니다. id=" + id);
    }
}
