package com.ebiz.wsb.domain.mail.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class SendAuthCodeRequest {

    @NotNull
    private String email;

}
