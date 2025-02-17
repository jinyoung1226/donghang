package com.ebiz.wsb.domain.notification.exception;

public class PushNotFoundException extends RuntimeException {
    public PushNotFoundException(String message) {
        super(message);
    }
}
