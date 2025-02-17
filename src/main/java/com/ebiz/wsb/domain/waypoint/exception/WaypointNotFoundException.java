package com.ebiz.wsb.domain.waypoint.exception;

public class WaypointNotFoundException extends RuntimeException {
    public WaypointNotFoundException(String message) {
        super(message);
    }
}
