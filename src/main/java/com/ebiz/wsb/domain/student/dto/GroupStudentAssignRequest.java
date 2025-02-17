package com.ebiz.wsb.domain.student.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class GroupStudentAssignRequest {
    private Long studentId;
    private Long groupId;
    private Long waypointId;
}
