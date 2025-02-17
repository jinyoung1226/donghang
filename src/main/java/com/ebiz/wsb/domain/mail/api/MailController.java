package com.ebiz.wsb.domain.mail.api;

import com.ebiz.wsb.domain.mail.application.MailService;
import com.ebiz.wsb.domain.mail.dto.CheckCodeDTO;
import com.ebiz.wsb.domain.mail.dto.SendAuthCodeRequest;
import com.ebiz.wsb.global.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailController {

    private static final String SEND_AUTH_CODE_MESSAGE = "인증번호가 전송되었습니다";
    private static final String MAIL_AUTH_SUCCESS_MESSAGE = "인증되었습니다";
    private static final String AUTH_MAIL_SUBJECT = "동행 가입 인증번호";
    private final MailService mailService;

    @PostMapping("/auth")
    public ResponseEntity<BaseResponse> sendAuthenticationCode(
            @Valid @RequestBody SendAuthCodeRequest sendAuthCodeRequest) {

        mailService.sendAuthenticationCode(AUTH_MAIL_SUBJECT, sendAuthCodeRequest.getEmail());
        return ResponseEntity.ok(BaseResponse.builder()
                .message(SEND_AUTH_CODE_MESSAGE)
                .build());
    }

    @PostMapping("/check")
    public ResponseEntity<BaseResponse> checkAuthCode(
            @Valid @RequestBody CheckCodeDTO checkCodeDTO) {
        mailService.checkAuthCode(checkCodeDTO);
        return ResponseEntity.ok(BaseResponse.builder()
                .message(MAIL_AUTH_SUCCESS_MESSAGE)
                .build());
    }
}
