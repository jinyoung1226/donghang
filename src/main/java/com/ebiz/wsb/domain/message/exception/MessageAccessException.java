package com.ebiz.wsb.domain.message.exception;

import com.ebiz.wsb.domain.message.entity.Message;

public class MessageAccessException extends RuntimeException{
    public MessageAccessException(String message){
        super(message);
    }
}
