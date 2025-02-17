package com.ebiz.wsb.domain.alert.exception;

public class AlertNotFoundException extends RuntimeException{
    public AlertNotFoundException(String message) {
        super(message);
    }
}
