package com.ebiz.wsb.domain.waypoint.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WaypointDTO {

    private Long waypointId;
    private String waypointName;
    private Double latitude;
    private Double longitude;
    private Integer waypointOrder;
    private Long groupId;
    private int studentCount;
    private Boolean attendanceComplete;
    private Integer currentCount;


}
