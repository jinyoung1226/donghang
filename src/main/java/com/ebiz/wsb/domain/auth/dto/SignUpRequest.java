package com.ebiz.wsb.domain.auth.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class SignUpRequest {
    private String email;
    private String password;
    private UserType userType;
    private String name;
}

