package com.ebiz.wsb.domain.notification.api;


import com.ebiz.wsb.domain.alert.application.AlertService;
import com.ebiz.wsb.domain.alert.entity.Alert;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushRequest;
import com.ebiz.wsb.domain.notification.dto.PushType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    @PostMapping("/send-group")
    public ResponseEntity<String> sendGroupNotification(@RequestBody Map<String, Object> request) {
        Long groupId = Long.valueOf(request.get("groupId").toString());
        String title = (String) request.get("title");
        String body = (String) request.get("body");
        PushType pushType = PushType.valueOf((String) request.get("pushType"));

        pushNotificationService.sendPushNotificationToGroup(groupId, title, body, pushType);
        return ResponseEntity.ok("그룹에 푸시 알림이 성공적으로 전송되었습니다.");
    }
}