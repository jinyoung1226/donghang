package com.ebiz.wsb.domain.notice.exception;

public class CommentAccessDeniedException extends RuntimeException{
    public CommentAccessDeniedException(String message){
        super(message);
    }
}
