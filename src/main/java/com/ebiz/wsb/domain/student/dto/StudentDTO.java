package com.ebiz.wsb.domain.student.dto;

import com.ebiz.wsb.domain.attendance.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDTO {
    private Long studentId;
    private Long groupId;
    private String name;
    private String schoolName;
    private String grade;
    private String notes;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String imagePath;
    private Long waypointId;
    private String waypointName;
    private AttendanceStatus attendanceStatus;
    private Long parentId;
    private String parentPhone;
    private String groupName;
}
