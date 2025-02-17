package com.ebiz.wsb.domain.group.exception;

public class GroupAlreadyActiveException extends RuntimeException {
    public GroupAlreadyActiveException(String message) {
        super(message);
    }
}
