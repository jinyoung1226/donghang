package com.ebiz.wsb.domain.schedule.dto;

import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.dto.GuardianSummaryDTO;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.student.entity.Student;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleDTO {
    private Long scheduleId;
    private LocalDate day;
    private String time;
    @JsonInclude(Include.NON_NULL)
    private Long scheduleTypeId;
    private String scheduleType;
    private Long groupId;
    private String groupName;
    private List<GuardianSummaryDTO> guardians;
}
