package com.ebiz.wsb.domain.attendance.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttendanceStatus {
    UNCONFIRMED,  // 미인증
    PRESENT,      // 출석 완료
    ABSENT,       // 결석
    PREABSENT     // 사전 결석
}
