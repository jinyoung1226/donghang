package com.ebiz.wsb.domain.waypoint.application;

import com.ebiz.wsb.domain.attendance.entity.Attendance;
import com.ebiz.wsb.domain.attendance.entity.AttendanceStatus;
import com.ebiz.wsb.domain.attendance.repository.AttendanceRepository;
import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.waypoint.dto.WaypointDTO;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.ebiz.wsb.domain.waypoint.exception.WaypointAttendanceCompletionException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointNotAccessException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointNotFoundException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointWithoutStudentsException;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaypointService {

    private final WaypointRepository waypointRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final AttendanceRepository attendanceRepository;

    public List<WaypointDTO> getWaypoints() {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        Group group;

        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;
            group = guardian.getGroup();

            if (group == null) {
                throw new GroupNotFoundException("해당 지도사는 어떤 그룹에도 속해 있지 않습니다.");
            }
        } else if (userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;
            group = parent.getGroup();

            if (group == null) {
                throw new GroupNotFoundException("해당 학부모는 어떤 그룹에도 속해 있지 않습니다.");
            }
        } else {
            throw new WaypointNotAccessException("인증되지 않은 사용자는 경유지를 조회할 수 없습니다.");
        }

        List<Waypoint> waypoints = waypointRepository.findByGroup_Id(group.getId());
        if (waypoints.isEmpty()) {
            throw new WaypointNotFoundException("해당 그룹에 등록된 경유지가 없습니다.");
        }

        return waypoints.stream()
                .map(this::convertToDTOWithStudentCount)
                .collect(Collectors.toList());
    }

    private WaypointDTO convertToDTOWithStudentCount(Waypoint waypoint) {
        // Waypoint -> WaypointDTO 변환, 학생 수 포함
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


    public List<StudentDTO> getStudentByWaypoint(Long waypointId) {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if (userByContextHolder instanceof Guardian) {
            Guardian guardian = (Guardian) userByContextHolder;
            Group group = guardian.getGroup();
            if (group == null) {
                throw new GroupNotFoundException("해당 지도는 그룹에 속해 있지 않습니다.");
            }

            List<Waypoint> waypoints = waypointRepository.findByGroup_Id(group.getId());
            if (waypoints.isEmpty()) {
                throw new WaypointWithoutStudentsException("해당 그룹에 등록된 경유지가 없습니다.");
            }

            for (Waypoint waypoint : waypoints) {
                if (waypoint.getId().equals(waypointId)) {
                    List<Student> students = waypointRepository.findStudentsByWaypointId(waypointId);
                    if (students.isEmpty()) {
                        throw new WaypointWithoutStudentsException("해당 경유지에 배정된 학생을 찾을 수 없습니다.");
                    }

                    return students.stream()
                            .map(student -> {
                                // 출석 정보를 확인하여 없으면 생성
                                LocalDate today = LocalDate.now();
                                Attendance attendance = attendanceRepository.findByStudentAndAttendanceDate(student, today)
                                        .orElseGet(() -> {
                                            Attendance newAttendance = Attendance.builder()
                                                    .student(student)
                                                    .waypoint(student.getWaypoint())
                                                    .attendanceDate(today)
                                                    .attendanceStatus(AttendanceStatus.UNCONFIRMED) // 기본 상태 설정
                                                    .build();
                                            return attendanceRepository.save(newAttendance); // 저장 후 반환
                                        });

                                // 출석 정보와 학생을 DTO로 변환
                                return convertToStudentDTO(student, attendance);
                            })
                            .collect(Collectors.toList());
                }
            }
        }

        throw new WaypointNotAccessException("해당 경유지에 접근할 권한이 없습니다.");
    }

    // DTO 변환 로직만을 처리하는 메서드
    private StudentDTO convertToStudentDTO(Student student, Attendance attendance) {
        return StudentDTO.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .schoolName(student.getSchoolName())
                .parentPhone(student.getParentPhone())
                .grade(student.getGrade())
                .notes(student.getNotes())
                .imagePath(student.getImagePath())
                .groupId(student.getGroup().getId())
                .waypointId(student.getWaypoint().getId())
                .attendanceStatus(attendance.getAttendanceStatus())
                .build();
    }

}
