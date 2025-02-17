package com.ebiz.wsb.domain.attendance.dto;

import com.ebiz.wsb.domain.attendance.entity.AttendanceMessageType;
import com.ebiz.wsb.domain.attendance.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDTO {
    private AttendanceMessageType messageType;
    private Long attendanceId;
    private Long studentId;
    private Long waypointId;
    private AttendanceStatus attendanceStatus;
    private LocalDate attendanceDate;
    private Boolean parentNoticeAbsence;
    private Long guardianId;
    private String guardianName;
    private String waypointName;

}
