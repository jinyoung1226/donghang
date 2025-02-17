package com.ebiz.wsb.domain.attendance.exception;

public class AttendanceNotFoundException extends RuntimeException{
    public AttendanceNotFoundException(Long attendanceId){
        super("출결 정보를 찾을 수 없습니다." + attendanceId);
    }
}
