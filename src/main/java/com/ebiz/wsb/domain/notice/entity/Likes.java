package com.ebiz.wsb.domain.notice.entity;

import com.ebiz.wsb.domain.auth.dto.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@DynamicInsert
@Table(name = "Likes")
public class Likes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userid", nullable = false)
    private Long userId;

    @Column(name = "userType", nullable = false)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @ManyToOne
    @JoinColumn(name = "groupNoticeId", nullable = false)
    private GroupNotice groupNotice;

    @Column(name = "liked", nullable = false)
    private Boolean liked;
}
