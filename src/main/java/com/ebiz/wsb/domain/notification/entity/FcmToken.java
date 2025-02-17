package com.ebiz.wsb.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // 유저 ID로 지도사와 부모 구분

    @Column(name = "token")
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;
}
