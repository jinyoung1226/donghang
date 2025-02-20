package com.ebiz.wsb.domain.notification.api;


import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody PushRequest request) {
        try {
            pushNotificationService.sendPushMessage(request.getTitle(), request.getBody(), null, request.getToken());
            return ResponseEntity.ok("푸시 알림이 성공적으로 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("푸시 알림 전송 실패: " + e.getMessage());
        }
    }
}