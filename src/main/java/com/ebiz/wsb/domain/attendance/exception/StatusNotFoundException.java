package com.ebiz.wsb.domain.attendance.exception;

public class StatusNotFoundException extends RuntimeException{
    public StatusNotFoundException(String message){
        super(message);
    }
}
