package com.ebiz.wsb.domain.alert.entity;

import com.ebiz.wsb.domain.notification.entity.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@NotNull
@DynamicInsert
@Table(name = "alert")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usertype")
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private AlertCategory alertCategory;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_read")
    private boolean isRead;

    public enum AlertCategory {
        POST,
        APP,
        SCHEDULE,
        MESSAGE,
        PREABSENT_MESSAGE,
        START_WORK_PARENT,
        START_WORK_GUARDIAN,
        PICKUP,
        END_WORK_PARENT,
        END_WORK_GUARDIAN
    }

    public void read() {
        isRead = true;
    }

}
