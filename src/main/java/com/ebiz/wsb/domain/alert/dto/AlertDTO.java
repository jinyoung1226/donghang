package com.ebiz.wsb.domain.alert.dto;

import com.ebiz.wsb.domain.alert.entity.Alert;
import java.time.LocalDateTime;

import com.ebiz.wsb.domain.notification.entity.UserType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlertDTO {

    private Long id;
    private String title;
    private String content;
    private Alert.AlertCategory category;
    private LocalDateTime createdAt;
    private Long receiverId;
    private UserType userType;
    private boolean isRead;

}
