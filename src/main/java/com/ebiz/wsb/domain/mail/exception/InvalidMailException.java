package com.ebiz.wsb.domain.mail.exception;

public class InvalidMailException extends RuntimeException {
    public InvalidMailException(String message) {
        super(message);
    }
}
