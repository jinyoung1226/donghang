package com.ebiz.wsb.domain.schedule.exception;

public class ScheduleAccessException extends RuntimeException{
    public ScheduleAccessException(String message){
        super(message);
    }

    public ScheduleAccessException(String message, Throwable cause){
        super(message, cause);
    }
}
