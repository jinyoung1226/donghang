package com.ebiz.wsb.domain.token.api;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.auth.dto.SignInDTO;
import com.ebiz.wsb.domain.auth.dto.UserType;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.token.application.TokenService;
import com.ebiz.wsb.domain.token.repository.TokenRepository;
import com.ebiz.wsb.global.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/token")
public class TokenController {
    private static final String AUTO_SIGN_IN_SUCCESS_MESSAGE = "자동 로그인 성공";

    private final TokenRepository tokenRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenService tokenService;

    @PostMapping("/signIn")
    public ResponseEntity<SignInDTO> checkToken(@RequestHeader(name = "Authorization") String authorizationHeader) {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;
            String refreshToken = tokenRepository.findById(guardian.getId());
            String accessToken = tokenService.resolveToken(authorizationHeader);
            return ResponseEntity.ok(SignInDTO.builder()
                    .id(guardian.getId())
                    .message(AUTO_SIGN_IN_SUCCESS_MESSAGE)
                    .refreshToken(refreshToken)
                    .accessToken(accessToken)
                    .userType(UserType.GUARDIAN)
                    .build());
        } else if (userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;
            String refreshToken = tokenRepository.findById(parent.getId());
            String accessToken = tokenService.resolveToken(authorizationHeader);
            return ResponseEntity.ok(SignInDTO.builder()
                    .id(parent.getId())
                    .message(AUTO_SIGN_IN_SUCCESS_MESSAGE)
                    .refreshToken(refreshToken)
                    .accessToken(accessToken)
                    .userType(UserType.PARENT)
                    .build());
        } else {
            throw new IllegalArgumentException("유효하지 않은 타입입니다");
        }
    }

    @PostMapping("/re-issue")
    public ResponseEntity<BaseResponse> reIssueToken(@RequestHeader(name = "Authorization") String authorizationHeader) {
        tokenService.checkRefreshToken(authorizationHeader);
        SignInDTO signInDTO = tokenService.generateTokens();
        return ResponseEntity.ok(signInDTO);
    }

}
