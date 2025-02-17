package com.ebiz.wsb.domain.auth.dto;

import com.ebiz.wsb.global.dto.BaseResponse;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class SignInDTO extends BaseResponse{
    private Long id;
    private String accessToken;
    private String refreshToken;
    private String message;
    private String fcmToken;
    private UserType userType;
}
