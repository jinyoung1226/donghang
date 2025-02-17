package com.ebiz.wsb.domain.auth.api;

import com.ebiz.wsb.domain.auth.exception.DuplicatedSignUpException;
import com.ebiz.wsb.global.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = {AuthController.class})
public class AuthExceptionHandler {

    @ExceptionHandler(DuplicatedSignUpException.class)
    public ResponseEntity<?> handleDuplicatedSignUpException(DuplicatedSignUpException e) {
        ErrorResponse errorResponse = ErrorResponse.builder().message(e.getMessage()).build();
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}