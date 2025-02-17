package com.ebiz.wsb.domain.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikesResponseDTO {
    private Long groupNoticeId;
    private boolean liked;
    private int likesCount;
}
