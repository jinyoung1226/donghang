package com.ebiz.wsb.location;

import com.ebiz.wsb.domain.auth.application.JwtProvider;
import com.ebiz.wsb.domain.location.application.StompHandler;
import com.ebiz.wsb.domain.location.exception.InvalidLocationDataException;
import com.ebiz.wsb.domain.token.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class StompHandlerTest {
    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private Message<?> message;

    @Mock
    private MessageChannel channel;

    private StompHandler stompHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stompHandler = new StompHandler(jwtProvider);
    }

    @Test
    void testValidTokenAndLocationData() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("latitude", "37.7749");
        accessor.setNativeHeader("longitude", "-122.4194");
        accessor.setNativeHeader("accessToken", "valid-token");

        when(jwtProvider.validateToken("valid-token")).thenReturn(true);

        assertDoesNotThrow(() -> {
            stompHandler.preSend(message, channel);
        });
    }

    @Test
    void testInvalidToken() {
        // StompHeaderAccessor 생성 및 헤더 설정
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("accessToken", "invalid-token");
        accessor.setNativeHeader("latitude", "37.7749");  // 위도 값 추가
        accessor.setNativeHeader("longitude", "-122.4194");  // 경도 값 추가

        // StompHeaderAccessor로 Message 객체 생성
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtProvider.validateToken("invalid-token")).thenReturn(false);  // 유효하지 않은 토큰으로 설정

        // 예외가 발생하는지 확인
        assertThrows(InvalidTokenException.class, () -> {
            stompHandler.preSend(message, channel);
        });
    }

    @Test
    void testMissingLocationData() {
        // StompHeaderAccessor 생성 및 헤더 설정 (위도와 경도는 설정하지 않음)
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setNativeHeader("accessToken", "valid-token");

        // StompHeaderAccessor로 Message 객체 생성
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 토큰 검증: 유효한 토큰으로 설정
        when(jwtProvider.validateToken("valid-token")).thenReturn(true);

        // 예외 발생 여부 확인: 위도와 경도가 없으므로 InvalidLocationDataException이 발생해야 함
        assertThrows(InvalidLocationDataException.class, () -> {
            stompHandler.preSend(message, channel);
        });
    }
}
