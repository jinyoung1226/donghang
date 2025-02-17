package com.ebiz.wsb.domain.auth.application;

import com.ebiz.wsb.domain.auth.dto.SignInDTO;
import com.ebiz.wsb.domain.auth.dto.SignInRequest;
import com.ebiz.wsb.domain.auth.dto.SignUpRequest;
import com.ebiz.wsb.domain.auth.dto.UserType;
import com.ebiz.wsb.domain.auth.exception.DuplicatedSignUpException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.notification.entity.FcmToken;
import com.ebiz.wsb.domain.notification.repository.FcmTokenRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import com.ebiz.wsb.domain.token.application.TokenService;
import com.ebiz.wsb.domain.token.entity.BlackList;
import com.ebiz.wsb.domain.token.repository.BlackListRepository;
import com.ebiz.wsb.domain.token.repository.TokenRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final JwtProvider jwtProvider;
    private final GuardianRepository guardianRepository;
    private final ParentRepository parentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenRepository tokenRepository;
    private final TokenService tokenService;
    private final BlackListRepository blackListRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Transactional
    public void signUp(SignUpRequest request){
        try {
            if(request.getUserType() == UserType.GUARDIAN){
                Guardian guardian = Guardian.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .name(request.getName())
                        .build();
                guardianRepository.save(guardian);
            } else if(request.getUserType() == UserType.PARENT){
                Parent parent = Parent.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .name(request.getName())
                        .build();
                parentRepository.save(parent);
            } else {
                throw new IllegalArgumentException("유효하지 않은 유저 타입입니다.");
            }
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedSignUpException("이미 존재하는 이메일입니다.");
        }
    }


    public SignInDTO signIn(SignInRequest request){
        Authentication authentication = authenticate(request);
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        Object principal = authentication.getPrincipal();
        Long userId = null;
        String message = "로그인 성공";

        if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();

            if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUARDIAN"))) {
                Guardian guardian = guardianRepository.findGuardianByEmail(email)
                        .orElse(null);
                if (guardian != null) {
                    userId = guardian.getId();
                    tokenRepository.save(userId, refreshToken);
                }
                if (userId != null && request.getFcmToken() != null) {
                    FcmToken fcmToken = FcmToken.builder()
                            .userId(userId)
                            .token(request.getFcmToken())
                            .userType(com.ebiz.wsb.domain.notification.entity.UserType.GUARDIAN)
                            .build();
                    fcmTokenRepository.save(fcmToken);
                    log.info("Saved FCM token for guardian ID {}: {}", userId, request.getFcmToken());
                }
            } else if (authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_PARENT"))) {
                Parent parent = parentRepository.findParentByEmail(email)
                        .orElse(null);
                if (parent != null) {
                    userId = parent.getId();
                    tokenRepository.save(userId, refreshToken);
                }
                if (userId != null && request.getFcmToken() != null) {
                    FcmToken fcmToken = FcmToken.builder()
                            .userId(userId)
                            .token(request.getFcmToken())
                            .userType(com.ebiz.wsb.domain.notification.entity.UserType.PARENT)
                            .build();
                    fcmTokenRepository.save(fcmToken);
                    log.info("Saved FCM token for parent ID {}: {}", userId, request.getFcmToken());
                }
            }
        }

        return SignInDTO.builder()
                .id(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message(message)
                .fcmToken(request.getFcmToken())
                .build();
    }

    public void signOut(String authorizationHeader) {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        String refreshToken = tokenService.resolveToken(authorizationHeader);

        if(userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;
            tokenRepository.deleteId(guardian.getId());
            List<FcmToken> fcmTokens = fcmTokenRepository.findByUserIdAndUserType(guardian.getId(), com.ebiz.wsb.domain.notification.entity.UserType.GUARDIAN);
            fcmTokenRepository.deleteAll(fcmTokens);
            saveBlackList(refreshToken);
        } else if(userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;
            tokenRepository.deleteId(parent.getId());
            List<FcmToken> fcmTokens = fcmTokenRepository.findByUserIdAndUserType(parent.getId(), com.ebiz.wsb.domain.notification.entity.UserType.PARENT);
            fcmTokenRepository.deleteAll(fcmTokens);
            saveBlackList(refreshToken);
        }
    }

    private void saveBlackList(String refreshToken) {
        try {
            blackListRepository.save(BlackList.builder()
                    .refreshToken(refreshToken)
                    .build());
        } catch (DuplicateKeyException e) {
            log.info("이미 로그아웃 처리한 토큰: " + refreshToken);
        }
    }

    private Authentication authenticate(SignInRequest request) {
        Authentication authenticationToken = new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
        );
        return authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    }

    public void updatePassword(String email, String password) {
        Optional<Parent> optionalParent = parentRepository.findParentByEmail(email);

        Optional<Guardian> optionalGuardian = guardianRepository.findGuardianByEmail(email);

        if(optionalParent.isPresent()) {
            Parent parent = optionalParent.get();
            Parent updatedParent = Parent.builder()
                    .id(parent.getId())
                    .name(parent.getName())
                    .email(parent.getEmail())
                    .password(passwordEncoder.encode(password))
                    .phone(parent.getPhone())
                    .address(parent.getAddress())
                    .imagePath(parent.getImagePath())
                    .group(parent.getGroup())
                    .build();
            parentRepository.save(updatedParent);
        } else if (optionalGuardian.isPresent()) {
            Guardian guardian = optionalGuardian.get();
            Guardian updatedGuardian = Guardian.builder()
                    .id(guardian.getId())
                    .name(guardian.getName())
                    .email(guardian.getEmail())
                    .password(passwordEncoder.encode(password))
                    .bio(guardian.getBio())
                    .experience(guardian.getExperience())
                    .phone(guardian.getPhone())
                    .imagePath(guardian.getImagePath())
                    .group(guardian.getGroup())
                    .build();
            guardianRepository.save(updatedGuardian);
        } else {
            throw new EntityNotFoundException("해당 이메일로 사용자를 찾을 수 없습니다.");
        }
    }
}
