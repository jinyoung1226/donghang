package com.ebiz.wsb.domain.attendance.api;

import com.ebiz.wsb.domain.attendance.application.AttendanceService;
import com.ebiz.wsb.domain.attendance.dto.AttendanceUpdateRequest;
import com.ebiz.wsb.global.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @MessageMapping("/group/{groupId}")
    public void updateAttendance(@Payload AttendanceUpdateRequest request, @DestinationVariable Long groupId) {
        attendanceService.updateAttendance(request.getStudentId(), request.getAttendanceStatus(), groupId);
    }

    @PostMapping("/{waypointId}/complete")
    public ResponseEntity<BaseResponse>  completeAttendance(@PathVariable Long waypointId) {
        BaseResponse baseResponse = attendanceService.completeAttendance(waypointId);
        return ResponseEntity.ok(baseResponse);
    }

    @PostMapping("/{studentId}/preabsent")
    public ResponseEntity<BaseResponse> preAbsent(@PathVariable Long studentId, @RequestParam("date") String date) {
        LocalDate absenceDate = LocalDate.parse(date);
        BaseResponse baseResponse = attendanceService.markPreAbsent(studentId, absenceDate);
        return ResponseEntity.ok(baseResponse);
    }
}