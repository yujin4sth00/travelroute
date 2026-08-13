package com.travelroute.backend.trip;

public class InvalidReorderRequestException extends RuntimeException {

    public InvalidReorderRequestException(String message) {
        super(message);
    }
}
