package com.ebiz.wsb.domain.guardian.exception;

public class GuardianNotFoundException extends RuntimeException{
    public GuardianNotFoundException(String message){
        super(message);
    }
}
