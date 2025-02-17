package com.ebiz.wsb.domain.auth.api;

import com.ebiz.wsb.domain.auth.application.AuthService;
import com.ebiz.wsb.domain.auth.dto.SignInDTO;
import com.ebiz.wsb.domain.auth.dto.SignInRequest;
import com.ebiz.wsb.domain.auth.dto.SignUpRequest;
import com.ebiz.wsb.global.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private static final String SIGN_UP_SUCCESS_MESSAGE = "회원가입 성공";
    private static final String RESET_PASSWORD_MESSAGE = "비밀번호가 변경되었습니다.";

    private final AuthService authService;

    @PostMapping("/signUp")
    public ResponseEntity<BaseResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        authService.signUp(signUpRequest);
        return new ResponseEntity<>(BaseResponse.builder()
                .message(SIGN_UP_SUCCESS_MESSAGE)
                .build(), HttpStatus.OK);
    }

    @PostMapping("/signIn")
    public ResponseEntity<SignInDTO> signIn(@Valid @RequestBody SignInRequest signInRequest) {
        SignInDTO signInDTO = authService.signIn(signInRequest);
        return new ResponseEntity<>(signInDTO, HttpStatus.OK);
    }

    @PostMapping("/signOut")
    public void signOut(@RequestHeader(name = "Authorization") String authorizationHeader) {
        authService.signOut(authorizationHeader);
    }

    @PutMapping("/reset_password")
    public ResponseEntity<BaseResponse> resetPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String password = requestBody.get("password");
        authService.updatePassword(email,password);
        return ResponseEntity.ok(BaseResponse.builder()
                .message(RESET_PASSWORD_MESSAGE)
                .build());
    }
}