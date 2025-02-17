package com.ebiz.wsb.domain.student.exception;

public class StudentNotAccessException extends RuntimeException{
    public StudentNotAccessException(String message){
        super(message);
    }
}
