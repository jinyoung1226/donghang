package com.ebiz.wsb.domain.token.application;

import com.ebiz.wsb.domain.auth.application.JwtProvider;
import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.auth.dto.SignInDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.token.entity.BlackList;
import com.ebiz.wsb.domain.token.exception.BlackListedTokenException;
import com.ebiz.wsb.domain.token.exception.DifferentRefreshTokenException;
import com.ebiz.wsb.domain.token.repository.BlackListRepository;
import com.ebiz.wsb.domain.token.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TokenService {
    private static final String DIFFERENT_REFRESH_TOKEN_EXCEPTION_MESSAGE = "다시 로그인 해주세요";
    private static final String BLACK_LISTED_TOKEN_EXCEPTION_MESSAGE = "잘못된 접근입니다. 다시 로그인 해주세요";

    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRepository tokenRepository;
    private final BlackListRepository blackListRepository;
    private final JwtProvider jwtProvider;

    public void checkRefreshToken(String authorizationHeader) {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if(userByContextHolder instanceof Guardian){
            Guardian guardian = (Guardian) userByContextHolder;
            String refreshToken = resolveToken(authorizationHeader);
            String refreshTokenInRedis = tokenRepository.findById(guardian.getId());
            if (refreshTokenInRedis == null || !refreshTokenInRedis.equals(refreshToken)) {
                deleteRefreshToken(guardian.getId());
                throw new DifferentRefreshTokenException(DIFFERENT_REFRESH_TOKEN_EXCEPTION_MESSAGE);
            }
            BlackList blackList = blackListRepository.findBlackListByRefreshToken(refreshToken);
            if (blackList != null) {
                throw new BlackListedTokenException(BLACK_LISTED_TOKEN_EXCEPTION_MESSAGE);
            }
        } else if (userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;
            String refreshToken = resolveToken(authorizationHeader);
            String refreshTokenInRedis = tokenRepository.findById(parent.getId());
            if (refreshTokenInRedis == null || !refreshTokenInRedis.equals(refreshToken)) {
                deleteRefreshToken(parent.getId());
                throw new DifferentRefreshTokenException(DIFFERENT_REFRESH_TOKEN_EXCEPTION_MESSAGE);
            }
            BlackList blackList = blackListRepository.findBlackListByRefreshToken(refreshToken);
            if (blackList != null) {
                throw new BlackListedTokenException(BLACK_LISTED_TOKEN_EXCEPTION_MESSAGE);
            }
        }
    }

    public SignInDTO generateTokens() {
        Authentication authentication = userDetailsService.getAuthentication();
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if(userByContextHolder instanceof Guardian){
            Guardian guardian = (Guardian) userByContextHolder;
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);
            tokenRepository.save(guardian.getId(), refreshToken);
            return SignInDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else if(userByContextHolder instanceof Parent){
            Parent parent = (Parent) userByContextHolder;
            String accessToken = jwtProvider.generateAccessToken(authentication);
            String refreshToken = jwtProvider.generateRefreshToken(authentication);
            tokenRepository.save(parent.getId(), refreshToken);
            return SignInDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 타입입니다.");
        }
    }

    public void deleteRefreshToken(Long memberId) {
        tokenRepository.deleteId(memberId);
    }

    public String resolveToken(String tokenHeader) {
        if (StringUtils.hasText(tokenHeader) && tokenHeader.startsWith("Bearer")) {
            return tokenHeader.substring(7);
        }
        return null;
    }
}
