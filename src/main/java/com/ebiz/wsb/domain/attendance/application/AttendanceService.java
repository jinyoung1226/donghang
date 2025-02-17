package com.ebiz.wsb.domain.attendance.application;

import com.ebiz.wsb.domain.attendance.dto.AttendanceDTO;
import com.ebiz.wsb.domain.attendance.entity.Attendance;
import com.ebiz.wsb.domain.attendance.entity.AttendanceMessageType;
import com.ebiz.wsb.domain.attendance.entity.AttendanceStatus;
import com.ebiz.wsb.domain.attendance.repository.AttendanceRepository;
import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.exception.GuideNotStartedException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.message.entity.Message;
import com.ebiz.wsb.domain.message.repository.MessageRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.StudentNotFoundException;
import com.ebiz.wsb.domain.student.repository.StudentRepository;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.ebiz.wsb.domain.waypoint.exception.IncompletePreviousWaypointException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointAttendanceCompletionException;
import com.ebiz.wsb.domain.waypoint.exception.WaypointNotFoundException;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import com.ebiz.wsb.global.dto.BaseResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final SimpMessagingTemplate template;
    private final WaypointRepository waypointRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final GroupRepository groupRepository;
    private final PushNotificationService pushNotificationService;
    private final MessageRepository messageRepository;

    @Transactional
    public void updateAttendance(Long studentId, AttendanceStatus attendanceStatus, Long groupId) {
        // "출근하기"를 누르지 않았다면, 출결 변경하지 못하게 막기
        Group checkGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다"));

        if(!checkGroup.getIsGuideActive()) {
            throw new GuideNotStartedException("출근하지 않았기 때문에 출결 변경 신청을 할 수 없습니다");
        }

        // 해당 경유지의 전체 출석 여부가 출석 완료 상태면, 변경 하지 못하게 막기
        Student checkStudent = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다"));

        if(checkStudent.getWaypoint().getAttendanceComplete()) {
            throw new WaypointAttendanceCompletionException("출석 완료 상태로, 변경이 불가능합니다");
        }

        LocalDate today = LocalDate.now();

        // 학생 정보 조회
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없습니다"));

        // 오늘 날짜에 해당하는 출석 정보 조회
        Attendance attendance = attendanceRepository.findByStudentAndAttendanceDate(student, today)
                .orElseGet(() -> Attendance.builder()
                        .student(student)
                        .waypoint(student.getWaypoint())
                        .attendanceDate(today)
                        .attendanceStatus(AttendanceStatus.UNCONFIRMED)
                        .build());

        // 이미 출석 상태가 동일한 경우 중복 처리를 방지
        if (attendance.getAttendanceStatus() == attendanceStatus) {
            // 상태가 동일하다면 아무 작업도 하지 않고 반환
            log.info("중복된 출석 상태 업데이트 요청이 감지되었습니다. 처리하지 않습니다.");
            return;
        }

        // 경유지에도 현재 "출석완료"인 학생 수 반영
        Waypoint waypoint = waypointRepository.findById(attendance.getWaypoint().getId())
                .orElseThrow(() -> new WaypointNotFoundException("해당 경유지를 찾을 수 없습니다"));

        // 경유지에도 현재 "출석완료"인 학생 수 반영
        Waypoint updatedWaypoint = waypoint.toBuilder()  // 새롭게 빌드된 객체를 updatedWaypoint에 저장
                .currentCount(attendanceStatus == AttendanceStatus.PRESENT
                        ? waypoint.getCurrentCount() + 1
                        : waypoint.getCurrentCount() - 1)
                .build();

        waypointRepository.save(updatedWaypoint); // 새롭게 빌드된 객체를 저장


        // 새로운 상태로 출석 정보 업데이트
        Attendance updatedAttendance = attendance.toBuilder()
                .attendanceStatus(attendanceStatus)
                .build();

        Attendance save = attendanceRepository.save(updatedAttendance);

        AttendanceDTO attendanceDTO = AttendanceDTO.builder()
                .attendanceId(save.getAttendanceId())
                .studentId(save.getStudent().getStudentId())
                .waypointId(save.getWaypoint().getId())
                .attendanceDate(save.getAttendanceDate())
                .attendanceStatus(save.getAttendanceStatus())
                .messageType(AttendanceMessageType.ATTENDANCE_CHANGE)
                .build();


        template.convertAndSend("/sub/group/" + groupId, attendanceDTO);
    }

    @Transactional
    public BaseResponse completeAttendance(Long waypointId) {
        // "출근하기"를 누르지 않았다면, 출석 완료 신청 못하게 막기
        Waypoint currentWaypoint = waypointRepository.findById(waypointId)
                .orElseThrow(() -> new WaypointNotFoundException("해당 경유지를 찾을 수 없습니다"));
        Group group = groupRepository.findById(currentWaypoint.getGroup().getId())
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다"));

        if(!group.getIsGuideActive()) {
            throw new GuideNotStartedException("출근하지 않았기 때문에 출석 완료 신청을 할 수 없습니다");
        }

        // 이전 경유지의 출석 상태가 완료되지 않았다면, 출석 완료를 하지 못하게 막기
        List<Waypoint> waypoints = group.getWaypoints();
        for (Waypoint waypoint : waypoints) {
            if (waypoint.getId() < waypointId && !waypoint.getAttendanceComplete()) {
                throw new IncompletePreviousWaypointException("이전 경유지의 출석이 완료되지 않아 출석 완료 신청을 할 수 없습니다");
            }
        }

        // 해당 경유지의 전체 출석 여부가 출석 완료 상태면, 변경 하지 못하게 막음
        Waypoint waypoint = waypointRepository.findById(waypointId)
                .orElseThrow(() -> new WaypointNotFoundException("해당 경유지를 찾을 수 없습니다"));

        if(waypoint.getAttendanceComplete()) {
            throw new WaypointAttendanceCompletionException("출석 완료 상태로, 변경이 불가능합니다");
        }

        // 해당 경유지의 오늘자 출석 정보 조회
        List<Attendance> attendances = attendanceRepository.findByWaypoint_IdAndAttendanceDate(waypoint.getId(), LocalDate.now());

        // 출석완료 버튼을 누르면, "미인증"인 학생을 "결석"으로 처리
        List<Attendance> updatedAttendances = attendances.stream()
                .map(attendance -> {
                    log.info(attendance.getAttendanceStatus().toString());
                    if (attendance.getAttendanceStatus() == AttendanceStatus.UNCONFIRMED) {
                        // 미인증 상태인 학생을 결석으로 변경
                        return attendance.toBuilder()
                                .attendanceStatus(AttendanceStatus.ABSENT)
                                .build();
                    }
                    return attendance; // 변경 없는 경우 기존 객체 반환
                })
                .collect(Collectors.toList());

        // 출석 완료 버튼 누를 때, "출석완료"인 학생만 count 하기 위한 변수
        int currentCount = (int) updatedAttendances.stream()
                .filter(attendance -> attendance.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();

        // "미인증"을 "결석"으로 처리 후 저장
        attendanceRepository.saveAll(updatedAttendances);

        // 경유지에 대한 출석 완료 여부를 false -> true로 변경하고, 출석 학생 수 업데이트
        Waypoint updatedWaypoint = waypoint.toBuilder()
                .attendanceComplete(true)
                .currentCount(currentCount)
                .build();

        // 출석 여부를 변경한 경유지 자체를 업데이트 및 저장
        waypointRepository.saveAndFlush(updatedWaypoint);

        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (!(userByContextHolder instanceof Guardian)) {
            throw new GuardianNotFoundException("해당 지도사를 찾을 수 없습니다");
        }
        Guardian guardian = (Guardian) userByContextHolder;

        // "출석완료" 했다는 것을 모든 인솔자에게 공지하기 위해 웹소캣으로 전송
        Long groupId = guardian.getGroup().getId();

        AttendanceDTO attendanceDTO = AttendanceDTO.builder()
                .messageType(AttendanceMessageType.ATTENDANCE_COMPLETE)
                .guardianId(guardian.getId())
                .guardianName(guardian.getName())
                .waypointName(updatedWaypoint.getWaypointName())
                .waypointId(updatedWaypoint.getId())
                .build();

        Map<String, String> pushData = pushNotificationService.createPushData(PushType.PICKUP);

        // 푸시 알림 body 내용에 한국 시간(Asia/Seoul)으로 시간 삽입
        LocalTime nowInKorea = LocalTime.now();
        String bodyWithTime = String.format(pushData.get("body"), nowInKorea.getHour(), nowInKorea.getMinute());
        pushData.put("body", bodyWithTime);

        // 알림 센터 body 내용에 한국 시간(Asia/Seoul)으로 시간 삽입
        LocalDateTime now = LocalDateTime.now();
        String alarmBodyWithTime = String.format(pushData.get("parent_alarm_center_body"), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute());
        pushData.put("parent_alarm_center_body", alarmBodyWithTime);

        pushNotificationService.sendPushNotificationToParentsAtWaypoint(waypointId, pushData.get("title"), pushData.get("body"), pushData.get("parent_alarm_center_title"), pushData.get("parent_alarm_center_body"), PushType.PICKUP);

        template.convertAndSend("/sub/group/" + groupId, attendanceDTO);

        return BaseResponse.builder()
                .message("해당 경유지의 출석을 완료하였습니다")
                .build();
    }

    @Transactional
    public BaseResponse markPreAbsent(Long studentId, LocalDate absenceDate) {
        // 현재 사용자 정보(인증 객체)로 학부모 여부 확인
        Object userByContextHolder = userDetailsService.getUserByContextHolder();

        if (!(userByContextHolder instanceof Parent)) {
            throw new ParentNotFoundException("해당 기능은 학부모만 사용할 수 있습니다.");
        }

        Parent parent = (Parent) userByContextHolder;

        // 학부모가 신청하는 자녀가 맞는지 검증
        Student findStudent = studentRepository.findById(studentId)
                .filter(student -> student.getParent().getId().equals(parent.getId())) // 학부모의 자녀인지 확인
                .orElseThrow(() -> new StudentNotFoundException("학생 정보를 찾을 수 없거나 자녀가 아닙니다."));

        // 신청한 날짜에 해당하는 출석 정보 조회, 없으면 새로 생성
        Attendance attendance = attendanceRepository.findByStudentAndAttendanceDate(findStudent, absenceDate)
                .orElseGet(() -> Attendance.builder()
                        .student(findStudent)
                        .waypoint(findStudent.getWaypoint())
                        .attendanceDate(absenceDate)  // 신청한 날짜로 설정
                        .attendanceStatus(AttendanceStatus.UNCONFIRMED) // 기본 상태 설정
                        .build());

        // 출석 상태가 이미 "사전 결석"인 경우 업데이트하지 않음
        if (attendance.getAttendanceStatus() != AttendanceStatus.PREABSENT) {
            Attendance updateAttendance = attendance.toBuilder()
                    .attendanceStatus(AttendanceStatus.PREABSENT)
                    .build();

            attendanceRepository.save(updateAttendance);

            // 요일 가져오기
            DayOfWeek dayOfWeek = absenceDate.getDayOfWeek();
            String dayOfWeekKorean = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN);
            int monthValue = absenceDate.getMonthValue();
            int dayOfMonth = absenceDate.getDayOfMonth();

            String content = String.format("%s 어머니께서 [%s: %d월 %d일 %s] 결석을 신청했어요.",
                    findStudent.getName(),
                    findStudent.getName(),
                    monthValue,
                    dayOfMonth,
                    dayOfWeekKorean);

            // 그룹 내 모든 인솔자에게 메시지 생성 및 저장
            List<Guardian> guardians = parent.getGroup().getGuardians();
            for (Guardian guardian : guardians) {
                Message message = Message.builder()
                        .group(parent.getGroup())
                        .parent(parent)
                        .content(content)
                        .transferredAt(LocalDateTime.now())
                        .isRead(false)
                        .guardian(guardian) // 각 메시지를 인솔자와 연결
                        .student(findStudent)
                        .build();

                messageRepository.save(message);
            }

            Map<String, String> pushData = pushNotificationService.createPushData(PushType.PREABSENT_MESSAGE);

            // title 내용에 학생 이름과 날짜 삽입
            String bodyWithStudentNameAndDate = String.format(pushData.get("title"), findStudent.getName(), absenceDate.getMonthValue(), absenceDate.getDayOfMonth());
            pushData.put("title", bodyWithStudentNameAndDate);

            // 요일을 한글로 출력
            String koreanDayOfWeek = dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN);
            // body 내용에 학생 이름과 해당 학생의 경유지, 날짜 삽입
            String bodyWithStudentNameAndWaypointAndDate = String.format(pushData.get("body"), findStudent.getName(), findStudent.getWaypoint().getWaypointName(), absenceDate.getMonthValue(), absenceDate.getDayOfMonth(), koreanDayOfWeek);
            pushData.put("body", bodyWithStudentNameAndWaypointAndDate);

            // 알림센터에서 지도사가 받는 body 값 수정
            String alarmBodyWithTime = String.format(pushData.get("guardian_alarm_center_body"), findStudent.getName(), monthValue, dayOfMonth, dayOfWeekKorean);
            pushData.put("guardian_alarm_center_body", alarmBodyWithTime);

            pushNotificationService.sendPushNotificationToGuardians(findStudent.getGroup().getId(), pushData.get("title"), pushData.get("body"), pushData.get("guardian_alarm_center_title"), pushData.get("guardian_alarm_center_body"), PushType.PREABSENT_MESSAGE);

            return BaseResponse.builder()
                    .message("사전 결석 신청이 완료되었습니다")
                    .build();
        } else {
            return BaseResponse.builder()
                    .message("이미 해당 날짜에 결석 신청이 있습니다")
                    .build();
        }
    }
}