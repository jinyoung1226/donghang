package com.ebiz.wsb.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "GroupNoticePhoto")
public class GroupNoticePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_notice_id", nullable = false)
    private GroupNotice groupNotice;

    @Column(name = "photo_url")
    private String photoUrl;
}