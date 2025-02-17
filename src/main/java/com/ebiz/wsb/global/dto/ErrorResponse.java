package com.ebiz.wsb.global.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ErrorResponse {
    private String message;
}