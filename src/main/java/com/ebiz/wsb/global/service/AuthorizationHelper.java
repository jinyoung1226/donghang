package com.ebiz.wsb.global.service;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotAccessException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotAccessException;
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

    private final UserDetailsServiceImpl userDetailsService;
    private final ParentRepository parentRepository;
    private final GuardianRepository guardianRepository;

    public Object getCurrentUser(String userType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("사용자가 인증되지 않았습니다.");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        log.info("Authenticated username: {}", username);

        if ("PARENT".equalsIgnoreCase(userType)) {
            return parentRepository.findParentByEmail(username)
                    .orElseThrow(() -> new ParentNotFoundException("학부모 정보를 찾을 수 없습니다."));
        } else if ("GUARDIAN".equalsIgnoreCase(userType)) {
            return guardianRepository.findGuardianByEmail(username)
                    .orElseThrow(() -> new GuardianNotFoundException("지도사 정보를 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자 타입입니다.");
        }
    }



    public Parent getLoggedInParent() {
        Object currentUser = getCurrentUser("PARENT");
        if (currentUser instanceof Parent) {
            log.info("Logged in Parent: {}", ((Parent) currentUser).getId());
            return (Parent) currentUser;
        }
        log.warn("Current user is not a Parent. CurrentUser: {}", currentUser);
        throw new ParentAccessException("학부모만 이 작업을 수행할 수 있습니다.");
    }

    public Guardian getLoggedInGuardian() {
        Object currentUser = getCurrentUser("GUARDIAN");
        if (currentUser instanceof Guardian) {
            return (Guardian) currentUser;
        }
        throw new GuardianNotAccessException("지도사만 이 작업을 수행할 수 있습니다.");
    }

    public void validateParentAccess(Parent parent, Long parentId) {
        Parent loggedInParent = getLoggedInParent();
        if (!parent.getId().equals(loggedInParent.getId())) {
            throw new ParentAccessException("본인의 정보만 조회할 수 있습니다.");
        }
    }

    public void validateGuardianAccess(Guardian guardian, Long guardianId) {
        if (!guardian.getId().equals(guardianId)) {
            throw new GuardianNotAccessException("권한이 없습니다.");
        }
    }


    public void validateStudentAccess(Student student, Object currentUser) {
        if (currentUser instanceof Parent parent) {
            // 부모는 자신의 자녀만 접근 가능
            if (!student.getParent().getId().equals(parent.getId())) {
                throw new StudentNotAccessException("본인의 자녀가 아닙니다.");
            }
        } else if (currentUser instanceof Guardian guardian) {
            // 지도사는 같은 그룹의 학생만 접근 가능
            if (student.getGroup() == null || !student.getGroup().getId().equals(guardian.getGroup().getId())) {
                throw new StudentNotAccessException("해당 그룹의 학생이 아닙니다.");
            }
        } else {
            throw new StudentNotAccessException("학생 정보를 조회할 권한이 없습니다.");
        }
    }
}
