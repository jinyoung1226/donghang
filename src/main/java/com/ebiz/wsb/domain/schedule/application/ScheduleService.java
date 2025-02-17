package com.ebiz.wsb.domain.schedule.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.dto.GuardianSummaryDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.schedule.dto.*;
import com.ebiz.wsb.domain.schedule.entity.Schedule;
import com.ebiz.wsb.domain.schedule.entity.ScheduleType;
import com.ebiz.wsb.domain.schedule.exception.ScheduleAccessException;
import com.ebiz.wsb.domain.schedule.exception.ScheduleNotFoundException;
import com.ebiz.wsb.domain.schedule.repository.ScheduleRepository;


import com.ebiz.wsb.domain.schedule.repository.ScheduleTypeRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Builder
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final GuardianRepository guardianRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final GroupRepository groupRepository;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public ScheduleDTO createSchedule(ScheduleDTO scheduleDTO) {
        Group group = groupRepository.findById(scheduleDTO.getGroupId())
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다."));

        List<Guardian> guardians = guardianRepository.findAllById(
                scheduleDTO.getGuardians().stream()
                        .map(GuardianSummaryDTO::getId)
                        .collect(Collectors.toList())
        );

        ScheduleType scheduleType = scheduleTypeRepository.findById(scheduleDTO.getScheduleTypeId())
                .orElseThrow(() -> new ScheduleNotFoundException("유효한 스케줄 유형 ID가 아닙니다."));

        Schedule schedule = Schedule.builder()
                .group(group)
                .guardians(guardians)
                .scheduleType(scheduleType)
                .day(scheduleDTO.getDay())
                .time(LocalTime.parse(scheduleDTO.getTime()))
                .build();

        schedule = scheduleRepository.save(schedule);

        Map<String, String> pushData = pushNotificationService.createPushData(PushType.SCHEDULE);
        sendPushNotificationToGroup(group.getId(), pushData.get("title"), pushData.get("body"), PushType.SCHEDULE);

        return convertScheduleToDTO(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> getSchedulesByRoleAndDateRange(LocalDate startDate, LocalDate endDate) {
        Object user = userDetailsService.getUserByContextHolder();

        if (user instanceof Guardian) {
            Guardian guardian = (Guardian) user;
            return getGroupSchedules(guardian.getGroup().getId(), startDate, endDate);
        } else if (user instanceof Parent) {
            Parent parent = (Parent) user;
            return getParentSchedules(parent, startDate, endDate);
        } else {
            throw new UsernameNotFoundException("알 수 없는 유저타입");
        }
    }

    private List<ScheduleDTO> getGroupSchedules(Long groupId, LocalDate startDate, LocalDate endDate) {
        List<Schedule> schedules = scheduleRepository.findByGroupIdAndDayBetween(groupId, startDate, endDate);
        return schedules.stream().map(this::convertScheduleToDTO).collect(Collectors.toList());
    }

    private List<ScheduleDTO> getParentSchedules(Parent parent, LocalDate startDate, LocalDate endDate) {
        List<Long> childGroupIds = parent.getStudents().stream()
                .map(student -> student.getGroup().getId())
                .distinct()
                .collect(Collectors.toList());

        List<Schedule> schedules = scheduleRepository.findByGroupIdInAndDayBetween(childGroupIds, startDate, endDate);
        return schedules.stream().map(this::convertScheduleToDTO).collect(Collectors.toList());
    }

    @Transactional
    public ScheduleDTO updateSchedule(Long scheduleId, ScheduleDTO scheduleDTO) {

        Schedule existingSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("스케줄을 찾을 수 없습니다."));

        ScheduleType scheduleType = scheduleTypeRepository.findById(scheduleDTO.getScheduleTypeId())
                .orElseThrow(() -> new IllegalArgumentException("유효한 스케줄 유형 ID가 아닙니다."));

        Schedule updatedSchedule = Schedule.builder()
                .group(existingSchedule.getGroup())
                .guardians(existingSchedule.getGuardians())
                .scheduleType(scheduleType)
                .day(scheduleDTO.getDay())
                .time(LocalTime.parse(scheduleDTO.getTime()))
                .build();

        updatedSchedule = scheduleRepository.save(updatedSchedule);

        return convertScheduleToDTO(updatedSchedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {

        Schedule existingSchedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException("스케줄을 찾을 수 없습니다."));

        scheduleRepository.deleteById(scheduleId);
    }


    private ScheduleDTO convertScheduleToDTO(Schedule schedule) {
        return ScheduleDTO.builder()
                .scheduleId(schedule.getScheduleId())
                .day(schedule.getDay())
                .time(schedule.getTime().toString())
                .scheduleType(schedule.getScheduleType() != null ? schedule.getScheduleType().getName() : "N/A")
                .groupId(schedule.getGroup() != null ? schedule.getGroup().getId() : null)
                .groupName(schedule.getGroup() != null ? schedule.getGroup().getGroupName() : null)
                .guardians(schedule.getGuardians().stream()
                        .map(this::convertToGuardianSummaryDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    private GuardianSummaryDTO convertToGuardianSummaryDTO(Guardian guardian) {
        return GuardianSummaryDTO.builder()
                .id(guardian.getId())
                .name(guardian.getName())
                .imagePath(guardian.getImagePath())
                .build();
    }

    private void sendPushNotificationToGroup(Long groupId, String title, String body, PushType pushType) {
        pushNotificationService.sendPushNotificationToGroup(groupId, title, body, pushType);
    }
}
