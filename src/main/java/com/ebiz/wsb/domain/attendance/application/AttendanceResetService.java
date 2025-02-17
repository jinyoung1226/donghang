package com.ebiz.wsb.domain.attendance.application;
import com.ebiz.wsb.domain.attendance.repository.AttendanceRepository;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceResetService {

    private final AttendanceRepository attendanceRepository;
    private final WaypointRepository waypointRepository;
    private final GroupRepository groupRepository;


    @Value("${schedule.use}")
    private boolean useSchedule;

    // 매일 저녁 7시에 출석 상태 초기화
    @Scheduled(cron = "${schedule.cron}")
    public void resetAttendanceStatus() {
        // 모든 학생의 출석 상태를 초기화
        if(useSchedule) {
            log.info("저녁 7시 기준으로, 1. 출석 테이블(오늘과 어제 데이터) 2. 경유지 출석 완료 필드 3. 경유지 출석 인원 수 필드 초기화 4. 인솔하는 인솔자 id랑 isGuideActive 초기화");

            // 오늘과 어제의 날짜를 구함
            // 미래의 결석 신청한 날짜는 유지하기 위함
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            attendanceRepository.deleteByAttendanceDateIn(List.of(today, yesterday));

            // 모든 경유지의 출석 관련 필드를 초기화
            waypointRepository.resetTwoWaypointFields();

            // 그룹의 출근 담당 인솔자 id와 출근 여부 초기화
            groupRepository.resetGuideStatusForAllGroups();



        }
    }
}
