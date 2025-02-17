package com.ebiz.wsb.domain.parent.api;

import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.global.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {ParentController.class})
public class parentExceptionHandler {

    @ExceptionHandler(ParentNotFoundException.class)
    public ResponseEntity<?> handleParentNotFoundException(ParentNotFoundException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}
