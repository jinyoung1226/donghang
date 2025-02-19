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
import com.ebiz.wsb.domain.waypoint.dto.WaypointDTO;
import com.ebiz.wsb.domain.waypoint.dto.WaypointMapper;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.ebiz.wsb.domain.waypoint.exception.WaypointNotFoundException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointWithoutStudentsException;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import com.ebiz.wsb.global.exception.InvalidUserTypeException;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WaypointService {

    private final WaypointRepository waypointRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final AttendanceRepository attendanceRepository;
    private final AuthorizationHelper authorizationHelper;
    private final WaypointMapper waypointMapper;

    /**
     * 학부모 또는 지도사가 자신의 그룹의 경유지 정보를 조회하는 메서드
     * @return 경유지 List DTO
     */
    @Transactional(readOnly = true)
    public List<WaypointDTO> getWaypoints() {
        Object currentUser = authorizationHelper.getCurrentUser();

        Group group;
        if (currentUser instanceof Guardian guardian) {
            group = guardian.getGroup();
            if (group == null) {
                throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
            }
        } else if (currentUser instanceof Parent parent) {
            group = parent.getGroup();
            if (group == null) {
                throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
            }
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
        }

        List<Waypoint> waypoints = waypointRepository.findByGroup_Id(group.getId());
        if (waypoints.isEmpty()) {
            throw new WaypointNotFoundException("경유지 정보를 찾을 수 없습니다.");
        }

        return waypoints.stream()
                .map(waypoint -> waypointMapper.convertToDTOWithStudentCount(waypoint))
                .collect(Collectors.toList());
    }


    /**
     * 경유지 별 학생 List를 조회하는 메서드
     * @param waypointId 경유지 Id
     * @return 각 경유지 별 속해있는 학생 List DTO
     */
    public List<StudentDTO> getStudentByWaypoint(Long waypointId) {
        Guardian guardian = authorizationHelper.getLoggedInGuardian();
        Group group = guardian.getGroup();

        if (group == null) {
            throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
        }

        List<Waypoint> waypoints = waypointRepository.findByGroup_Id(group.getId());
        if (waypoints.isEmpty()) {
            throw new WaypointNotFoundException("경유지 정보를 찾을 수 없습니다.");
        }

        for (Waypoint waypoint : waypoints) {
            if (waypoint.getId().equals(waypointId)) {
                List<Student> students = waypointRepository.findStudentsByWaypointId(waypointId);
                if (students.isEmpty()) {
                    throw new WaypointWithoutStudentsException("경유지에 배정된 학생을 찾을 수 없습니다.");
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
                            return convertToStudentDTO(student, attendance);
                        })
                        .collect(Collectors.toList());
            }
        }
        throw new WaypointNotFoundException("경유지 정보를 찾을 수 없습니다.");
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
