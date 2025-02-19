package com.ebiz.wsb.domain.waypoint.dto;

import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import org.springframework.stereotype.Component;

@Component
public class WaypointMapper {

    public WaypointDTO convertToDTOWithStudentCount(Waypoint waypoint) {
        return new WaypointDTO(
                waypoint.getId(),
                waypoint.getWaypointName(),
                waypoint.getLatitude(),
                waypoint.getLongitude(),
                waypoint.getWaypointOrder(),
                waypoint.getGroup().getId(),
                waypoint.getStudents().size(),
                waypoint.getAttendanceComplete(),
                waypoint.getCurrentCount()
        );
    }
}
