package com.ebiz.wsb.domain.notification.dto;

import lombok.Data;

@Data
public class AndroidNotificationDTO {
    private NotificationDTO notification;
    private String priority = "high";
}
