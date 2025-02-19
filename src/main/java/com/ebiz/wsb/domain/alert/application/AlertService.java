package com.ebiz.wsb.domain.alert.application;

import com.ebiz.wsb.domain.alert.dto.AlertDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.notification.entity.UserType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.alert.entity.Alert;
import com.ebiz.wsb.domain.alert.repository.AlertRepository;
import com.ebiz.wsb.global.exception.InvalidUserTypeException;
import com.ebiz.wsb.global.service.AuthorizationHelper;
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
    private final AlertRepository alertRepository;
    private final AuthorizationHelper authorizationHelper;

    /**
     * 앱에서 알림을 보내는 기능을 진행할 때, 알림 데이터를 만들어주는 메서드
     * @param receiverId 알림을 받는 사용자 Id
     * @param category 알림의 카테고리
     * @param alarmTitle 알림 주제
     * @param alarmContent 알림 내용
     * @param userType 사용자 유저 타입
     */
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

    /**
     * 학부모 또는 지도사가 알림센터에 쌓인 데이터를 조회하는 메서드
     * @return 쌓인 알림 List DTO
     */
    @Transactional
    public List<AlertDTO> getAlerts() {
        Object currentUser = authorizationHelper.getCurrentUser();

        Long receiverId;
        UserType userType;

        if (currentUser instanceof Guardian guardian) {
            receiverId = guardian.getId();
            userType = UserType.GUARDIAN;
        } else if (currentUser instanceof Parent parent) {
            receiverId = parent.getId();
            userType = UserType.PARENT;
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
        }
        return getAlertsByReceiverIdAndUserType(receiverId, userType);
    }

    /**
     * 특정 사용자의 알림센터의 데이터를 전달해주는 private 메서드
     * @param receiverId 사용자 Id
     * @param userType 사용자 유저 타입
     * @return 알림 List DTO
     */
    private List<AlertDTO> getAlertsByReceiverIdAndUserType(Long receiverId, UserType userType) {
        // 사용자에게 온 메시지 리스트 받기
        List<Alert> alerts = alertRepository.findByReceiverIdAndUserType(receiverId, userType);
        alerts.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));

        // 조회할 알림 모아놓는 리스트
        List<AlertDTO> alertDTOs = new ArrayList<>();
        // 조회한 뒤, 읽음 처리할 알림 리스트
        List<Alert> unReadAlerts = new ArrayList<>();

        for (Alert alert : alerts) {
            if (!alert.isRead()) {
                unReadAlerts.add(alert);
            }

            alertDTOs.add(AlertDTO.builder()
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

        // 비동기 처리
        markAlertsAsReadAsync(unReadAlerts);
        return alertDTOs;
    }

    /**
     * 비동기로 안 읽은 알림은 조회한 뒤에 읽음 처리로 해주는 메서드
     * @param alerts 알림 List
     */
    @Async
    public void markAlertsAsReadAsync(List<Alert> alerts) {
        if (alerts.isEmpty()) return;
        List<Long> alertIds = alerts.stream()
                .map(Alert::getId)
                .toList();
        alertRepository.markAlertsAsRead(alertIds);
    }
}
