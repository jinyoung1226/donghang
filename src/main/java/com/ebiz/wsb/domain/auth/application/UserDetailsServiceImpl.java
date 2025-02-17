package com.ebiz.wsb.domain.auth.application;

import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import java.util.Collection;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final String USER_NOT_FOUND_EXCEPTION = "존재하지 않는 회원입니다.";

    private final GuardianRepository guardianRepository;
    private final ParentRepository parentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Guardian guardian = guardianRepository.findGuardianByEmail(username).orElse(null);
        Parent parent = parentRepository.findParentByEmail(username).orElse(null);

        if (guardian != null) {
            return new User(guardian.getEmail(), guardian.getPassword(), getAuthorities(guardian));
        } else if (parent != null) {
            return new User(parent.getEmail(), parent.getPassword(), getAuthorities(parent));
        } else {
            throw new UsernameNotFoundException(USER_NOT_FOUND_EXCEPTION);
        }
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Object user) {
        // 권한 정보를 GrantedAuthority 객체 컬렉션으로 반환
        // 여기서 user가 Guardian인지 Parent인지 확인할 필요는 없고, 역할에 따라 처리 가능
        if (user instanceof Guardian) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_GUARDIAN"));
        } else if (user instanceof Parent) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARENT"));
        } else {
            throw new IllegalArgumentException("Invalid user type");
        }
    }

    public Object getUserByContextHolder(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        Guardian guardian = guardianRepository.findGuardianByEmail(userDetails.getUsername()).orElse(null);
        Parent parent = parentRepository.findParentByEmail(userDetails.getUsername()).orElse(null);

        if (guardian != null) {
            return guardian;
        } else if (parent != null) {
            return parent;
        } else {
            throw new UsernameNotFoundException(USER_NOT_FOUND_EXCEPTION);
        }
    }

    public Object getUserTypeByContextHolder(String userType) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserDetails userDetails = (UserDetails) principal;
        String username = userDetails.getUsername();

        if ("PARENT".equalsIgnoreCase(userType)) {
            return parentRepository.findParentByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Parent를 찾을 수 없습니다."));
        } else if ("GUARDIAN".equalsIgnoreCase(userType)) {
            return guardianRepository.findGuardianByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Guardian을 찾을 수 없습니다."));
        } else {
            throw new IllegalArgumentException("Invalid user type: " + userType);
        }
    }

    public Authentication getAuthentication(){
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
