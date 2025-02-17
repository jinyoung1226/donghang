package com.ebiz.wsb.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageSendRequestDTO {
    private Long studentId;

    @Nullable
    private String content;
}
