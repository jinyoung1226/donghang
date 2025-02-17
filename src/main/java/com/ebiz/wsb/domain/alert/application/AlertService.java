package com.ebiz.wsb.domain.alert.application;

import com.ebiz.wsb.domain.alert.dto.AlertDTO;
import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.notification.entity.UserType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.alert.entity.Alert;
import com.ebiz.wsb.domain.alert.exception.AlertNotFoundException;
import com.ebiz.wsb.domain.alert.repository.AlertRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
    private final UserDetailsServiceImpl userDetailsService;
    private final AlertRepository alertRepository;

    @Transactional
    public void createAlert(Long receiverId, Alert.AlertCategory category, String alarmTitle, String alarmContent, UserType userType) {
        Alert alert = Alert.builder()
                .receiverId(receiverId)
                .alertCategory(category)
                .title(alarmTitle)
                .content(alarmContent)
                .createdAt(LocalDateTime.now())
                .isRead(false)
                .userType(userType)
                .build();

        alertRepository.save(alert);
    }

    @Transactional
    public List<AlertDTO> getAlertsForCurrentUser() {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;

            //자신에게 온 메시지 리스트 받기
            List<Alert> Alerts = alertRepository.findByReceiverIdAndUserType(guardian.getId(), UserType.GUARDIAN);

            // 메시지 리스트 최신 순이 가장 앞으로 오게끔 정렬
            Alerts.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));

            // 조회할 알림 모아놓는 리스트
            List<AlertDTO> alerts = new ArrayList<>();

            // 조회한 뒤, 읽음 처리할 알림 리스트
            List<Alert> unReadAlerts = new ArrayList<>();

            // DTO로 변경하여 데이터 응답
            for (Alert alert : Alerts) {
                if (!alert.isRead()) {
                    unReadAlerts.add(alert);
                }

                // AlertDTO 생성 및 리스트에 추가
                alerts.add(AlertDTO.builder()
                        .id(alert.getId())
                        .category(alert.getAlertCategory())
                        .content(alert.getContent())
                        .createdAt(alert.getCreatedAt())
                        .isRead(alert.isRead())
                        .receiverId(alert.getReceiverId())
                        .title(alert.getTitle())
                        .userType(alert.getUserType())
                        .build());
            }

            // 비동기 메서드를 통해 읽음 상태를 업데이트
            markAlertsAsReadAsync(unReadAlerts);

            return alerts;

        } else if (userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;

            //자신에게 온 메시지 리스트 받기
            List<Alert> Alerts = alertRepository.findByReceiverIdAndUserType(parent.getId(), UserType.PARENT);

            // 메시지 리스트 최신 순이 가장 앞으로 오게끔 정렬
            Alerts.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));

            // 조회할 알림 모아놓는 리스트
            List<AlertDTO> alerts = new ArrayList<>();

            // 조회한 뒤, 읽음 처리할 알림 리스트
            List<Alert> unReadAlerts = new ArrayList<>();

            // DTO로 변경하여 데이터 응답
            for (Alert alert : Alerts) {
                if (!alert.isRead()) {
                    unReadAlerts.add(alert);
                }

                // AlertDTO 생성 및 리스트에 추가
                alerts.add(AlertDTO.builder()
                        .id(alert.getId())
                        .category(alert.getAlertCategory())
                        .content(alert.getContent())
                        .createdAt(alert.getCreatedAt())
                        .isRead(alert.isRead())
                        .receiverId(alert.getReceiverId())
                        .title(alert.getTitle())
                        .userType(alert.getUserType())
                        .build());
            }

            // 비동기 메서드를 통해 읽음 상태를 업데이트
            markAlertsAsReadAsync(unReadAlerts);

            return alerts;

        } else {
            throw new IllegalArgumentException("유효하지 않은 유저 타입입니다.");
        }
    }

    @Async
    public void markAlertsAsReadAsync(List<Alert> alerts) {
        if (alerts.isEmpty()) return;

        List<Long> alertIds = alerts.stream()
                .map(Alert::getId)
                .toList();

        alertRepository.markAlertsAsRead(alertIds);
    }
}
