package com.ebiz.wsb.domain.waypoint.api;

import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.waypoint.application.WaypointService;
import com.ebiz.wsb.domain.waypoint.dto.WaypointDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/waypoints")
public class WaypointController {

    private final WaypointService waypointService;

    @GetMapping
    public ResponseEntity<List<WaypointDTO>> getWaypoints() {
        List<WaypointDTO> waypoints = waypointService.getWaypoints();
        return ResponseEntity.ok(waypoints);
    }

    @GetMapping("/{waypointId}/students")
    public ResponseEntity<List<StudentDTO>> getStudentsByWaypoint(@PathVariable Long waypointId) {
        List<StudentDTO> students = waypointService.getStudentByWaypoint(waypointId);
        return ResponseEntity.ok(students);
    }


}
