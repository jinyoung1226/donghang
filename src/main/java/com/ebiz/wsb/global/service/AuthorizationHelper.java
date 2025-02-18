package com.ebiz.wsb.global.service;

import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationHelper {

    private final ParentRepository parentRepository;
    private final GuardianRepository guardianRepository;

    public Object getCurrentUser(String userType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("인증된 유저가 아닙니다.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        if ("PARENT".equalsIgnoreCase(userType)) {
            return parentRepository.findParentByEmail(username)
                    .orElseThrow(() -> new ParentNotFoundException("학부모 정보를 찾을 수 없습니다."));
        } else if ("GUARDIAN".equalsIgnoreCase(userType)) {
            return guardianRepository.findGuardianByEmail(username)
                    .orElseThrow(() -> new GuardianNotFoundException("지도사 정보를 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("유효하지 않은 유저 타입입니다.");
        }
    }

    public Parent getLoggedInParent() {
        Object currentUser = getCurrentUser("PARENT");
        if (currentUser instanceof Parent) {
            return (Parent) currentUser;
        }
        throw new ParentNotFoundException("학부모 정보를 찾을 수 없습니다.");
    }

    public Guardian getLoggedInGuardian() {
        Object currentUser = getCurrentUser("GUARDIAN");
        if (currentUser instanceof Guardian) {
            return (Guardian) currentUser;
        }
        throw new GuardianNotFoundException("지도사 정보를 찾을 수 없습니다.");
    }
}
