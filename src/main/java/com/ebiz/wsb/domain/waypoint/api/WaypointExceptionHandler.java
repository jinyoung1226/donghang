package com.ebiz.wsb.domain.waypoint.api;

import com.ebiz.wsb.domain.attendance.api.AttendanceController;
import com.ebiz.wsb.domain.auth.exception.DuplicatedSignUpException;
import com.ebiz.wsb.domain.waypoint.exception.IncompletePreviousWaypointException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointAttendanceCompletionException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointWithoutStudentsException;
import com.ebiz.wsb.global.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {WaypointController.class, AttendanceController.class})
public class WaypointExceptionHandler {

    @ExceptionHandler(WaypointWithoutStudentsException.class)
    public ResponseEntity<?> handleWaypointWithoutStudentsException(WaypointWithoutStudentsException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WaypointAttendanceCompletionException.class)
    public ResponseEntity<?> handleWaypointAttendanceCompletionException(WaypointAttendanceCompletionException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncompletePreviousWaypointException.class)
    public ResponseEntity<?> handleIncompletePreviousWaypointException(IncompletePreviousWaypointException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
