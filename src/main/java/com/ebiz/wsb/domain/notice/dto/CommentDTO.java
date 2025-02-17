package com.ebiz.wsb.domain.notice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentDTO {
    private Long commentId;
    private String content;
    private Long guardianId;
    private String guardianName;
    private Long parentId;
    private String parentName;
    private LocalDateTime createdAt;
}
