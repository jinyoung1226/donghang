package com.ebiz.wsb.domain.schedule.dto;

import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleTypeDTO {
    private Long id;
    private String name;
}
