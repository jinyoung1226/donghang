package com.ebiz.wsb.domain.notice.dto;


import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.dto.GuardianSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class GroupNoticeDTO {
    private Long groupNoticeId;
    private String content;
    private List<String> photos;
    private Integer likes;
    private LocalDateTime createdAt;
    private GuardianSummaryDTO guardian;
    private boolean liked;
}
