package com.ebiz.wsb.domain.mail.exception;

public class WrongAuthenticationCodeException extends RuntimeException {
    public WrongAuthenticationCodeException(String message) {
        super(message);
    }
}
