package com.ebiz.wsb.domain.mail.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CheckCodeDTO {
    private String email;
    private String code;
}
