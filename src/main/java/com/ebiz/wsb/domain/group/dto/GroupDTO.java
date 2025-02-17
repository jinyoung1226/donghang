package com.ebiz.wsb.domain.group.dto;

import com.ebiz.wsb.domain.attendance.entity.AttendanceMessageType;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupDTO {

    private Long id;
    private String groupName;
    private String schoolName;
    private String regionName;
    private String districtName;
    private List<GuardianDTO> guardians;
    private Integer studentCount;
    private Boolean isGuideActive;
    private AttendanceMessageType messageType;
    private Boolean shuttleStatus;
    private String groupImage;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Long dutyGuardianId;
}
