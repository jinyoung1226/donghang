package com.ebiz.wsb.domain.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDTO {

    private Long noticeId;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
