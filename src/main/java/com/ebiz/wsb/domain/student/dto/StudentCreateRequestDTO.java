package com.ebiz.wsb.domain.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentCreateRequestDTO {
    private String name;
    private String schoolName;
    private String grade;
    private String notes;
    private Long parentId;
    private String parentPhone;
}