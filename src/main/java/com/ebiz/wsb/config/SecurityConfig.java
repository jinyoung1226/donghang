package com.ebiz.wsb.config;

import com.ebiz.wsb.domain.auth.CustomAuthenticationEntryPoint;
import com.ebiz.wsb.domain.auth.application.JwtProvider;
import com.ebiz.wsb.domain.auth.filter.AuthExceptionHandlerFilter;
import com.ebiz.wsb.domain.auth.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtProvider jwtProvider;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
        throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )
                .authorizeHttpRequests((auth) -> {
                    auth.requestMatchers("/healthy").permitAll();
                    auth.requestMatchers("/auth/*").permitAll();
                    auth.requestMatchers("/ws", "/ws/**", "/app/**", "/topic/**").permitAll();
                    auth.requestMatchers(("/mail/auth")).permitAll();
                    auth.requestMatchers(("/mail/check")).permitAll();
                    auth.anyRequest().authenticated();
                }
        ).addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new AuthExceptionHandlerFilter(), JwtAuthenticationFilter.class);
        http.exceptionHandling(manager -> manager.authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new AccessDeniedHandlerImpl()));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
