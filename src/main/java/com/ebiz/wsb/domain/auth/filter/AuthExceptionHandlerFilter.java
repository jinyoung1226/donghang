package com.ebiz.wsb.domain.auth.filter;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthExceptionHandlerFilter extends OncePerRequestFilter {

    private static final String LOGIN_AGAIN_MESSAGE = "다시 로그인 해주세요";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | MalformedJwtException e) {
            // 토큰의 유효기간 만료 또는 잘못된 토큰
            setErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
        } catch (SecurityException e) {
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 유효하지 않은 토큰
            setErrorResponse(response, ErrorCode.INVALID_TOKEN);
        }
    }

    private void setErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException{
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse errorResponse = new ErrorResponse(errorCode.getHttpStatus().value(), errorCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Data
    public static class ErrorResponse{
        private final Integer code;
        private final String message;
    }

    @Getter
    @RequiredArgsConstructor
    enum ErrorCode{
        INVALID_TOKEN(HttpStatus.UNAUTHORIZED, LOGIN_AGAIN_MESSAGE),
        EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, LOGIN_AGAIN_MESSAGE);

        private final HttpStatus httpStatus;
        private final String message;
    }
}
