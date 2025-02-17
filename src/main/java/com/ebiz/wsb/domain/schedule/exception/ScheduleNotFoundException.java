package com.ebiz.wsb.domain.schedule.exception;

public class ScheduleNotFoundException extends RuntimeException{
    public ScheduleNotFoundException(String message){
        super(message);
    }
}