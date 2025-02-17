package com.ebiz.wsb.domain.attendance.dto;

import com.ebiz.wsb.domain.attendance.entity.AttendanceStatus;
import lombok.*;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@ToString

public class AttendanceUpdateRequest {

    private Long studentId;
    private AttendanceStatus attendanceStatus;

}

