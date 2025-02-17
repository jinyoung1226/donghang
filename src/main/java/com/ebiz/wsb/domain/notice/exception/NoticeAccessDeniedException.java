package com.ebiz.wsb.domain.notice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NoticeAccessDeniedException extends RuntimeException{

    public NoticeAccessDeniedException(String message){
        super(message);
    }
}
