package com.ebiz.wsb.domain.token.exception;

public class DifferentRefreshTokenException extends RuntimeException {
    public DifferentRefreshTokenException(String message) {
        super(message);
    }
}
