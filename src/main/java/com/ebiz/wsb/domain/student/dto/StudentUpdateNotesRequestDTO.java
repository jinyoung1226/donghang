package com.ebiz.wsb.domain.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentUpdateNotesRequestDTO {
    private Long studentId;
    private String notes;
}
