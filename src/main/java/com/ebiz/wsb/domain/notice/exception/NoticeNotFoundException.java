package com.ebiz.wsb.domain.notice.exception;

public class NoticeNotFoundException extends RuntimeException{

    public NoticeNotFoundException(Long id){
        super(id + "의 공지를 찾을 수 없습니다.");
    }
}
