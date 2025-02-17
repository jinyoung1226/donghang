package com.ebiz.wsb.global.api;

import com.ebiz.wsb.domain.group.exception.GroupAlreadyActiveException;
import com.ebiz.wsb.domain.group.exception.GroupNotAccessException;
import com.ebiz.wsb.domain.group.exception.GuideNotOnDutyException;
import com.ebiz.wsb.domain.group.exception.GuideNotStartedException;
import com.ebiz.wsb.domain.guardian.exception.FileUploadException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotAccessException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.location.exception.InvalidLocationDataException;
import com.ebiz.wsb.domain.mail.exception.InvalidMailException;
import com.ebiz.wsb.domain.message.exception.MessageAccessException;
import com.ebiz.wsb.domain.notice.exception.*;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.schedule.exception.ScheduleAccessException;
import com.ebiz.wsb.domain.student.exception.StudentNotAccessException;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.token.exception.InvalidTokenException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointNotFoundException;
import com.ebiz.wsb.global.dto.BaseResponse;
import com.ebiz.wsb.global.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

// 특정 Exception 발생 시 해당 응답을 반환해 에러 메시지를 클라이언트한테 반환

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String WRONG_USER_EXCEPTION_MESSAGE = "이메일 혹은 비밀번호가 일치하지 않습니다";
    private static final String WRONG_REQUEST_EXCEPTION_MESSAGE = "잘못된 요청입니다";
    private static final String MAX_UPLOAD_SIZE_EXCEEDED_MESSAGE = "사진은 최대 10MB까지 업로드 가능합니다";

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(Exception ex) {
        log.error("Authentication error: {}", ex.getMessage());
        return new ResponseEntity<>(ErrorResponse.builder()
                .message("이메일 혹은 비밀번호가 일치하지 않습니다")
                .build(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity handleNullRequestException(MethodArgumentNotValidException e) {
        return new ResponseEntity(ErrorResponse.builder()
                .message(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoticeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoticeNotFoundException(NoticeNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message("파일 처리 중 오류가 발생했습니다: " + ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(MAX_UPLOAD_SIZE_EXCEEDED_MESSAGE)
                        .build());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException ex){
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder().
                        message("유효하지 않은 토큰입니다.")
                        .build());
    }

    @ExceptionHandler(InvalidLocationDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLocationDataException(InvalidLocationDataException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message("잘못된 위치 데이터입니다.")
                        .build());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<ErrorResponse> handleFileUploadException(FileUploadException ex){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(InvalidMailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMailException(InvalidMailException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(GuardianNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGuardianNotFoundException(GuardianNotFoundException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDeniedException(java.nio.file.AccessDeniedException ex){
        return new ResponseEntity<>("접근이 거부되었습니다." + ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ScheduleAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleScheduleAccessException(ScheduleAccessException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(LikesNumberException.class)
    public ResponseEntity<String> handleLikesNumberException(LikesNumberException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(GuideNotStartedException.class)
    public ResponseEntity<?> handleGuideNotStartedException(GuideNotStartedException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoticeAccessDeniedException.class)
    public ResponseEntity<Object> handleNoticeAccessDeniedException(NoticeAccessDeniedException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(WaypointNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWaypointNotFoundException(WaypointNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(StudentNotAccessException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotAccessException(StudentNotAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudentNotFoundException(StudentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(ParentAccessException.class)
    public ResponseEntity<ErrorResponse> handleParentAccessException(ParentAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(GuardianNotAccessException.class)
    public ResponseEntity<ErrorResponse> handleGuardianNotAccessException(GuardianNotAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            GroupAlreadyActiveException.class,
            GroupNotAccessException.class,
            GuideNotOnDutyException.class,
    })
    @ResponseBody
    public String handleBadRequestExceptions(RuntimeException ex) {
        return ex.getMessage();
    }

    @ExceptionHandler(ParentNotFoundException.class)
    public ResponseEntity<ErrorResponse> ParentNotFoundException(ParentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(MessageAccessException.class)
    public ResponseEntity<ErrorResponse> MesssageAccessException(MessageAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDenied(CommentAccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticatedUser(CommentNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .build());
    }
}