package com.ebiz.wsb.domain.notice.exception;

public class NotNoticeInGroupException extends RuntimeException{
    public NotNoticeInGroupException(Long groupId) {
        super("그룹 ID " + groupId + "에 대한 공지사항이 없습니다.");
    }
}
