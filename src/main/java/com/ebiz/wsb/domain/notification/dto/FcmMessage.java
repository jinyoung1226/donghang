package com.ebiz.wsb.domain.notification.dto;


import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FcmMessage {
    private Message message;

    @Builder
    @AllArgsConstructor
    @Getter
    public static class Message {
        private Map<String, String> data;
        private AndroidNotificationDTO android;
        private Notification notification;
        private String token;
    }

    @Builder
    @Getter
    public static class Notification{
        private Object title;
        private Object body;
    }
}
