package com.ebiz.wsb.domain.group.application;

import com.ebiz.wsb.domain.attendance.entity.AttendanceMessageType;
import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupAlreadyActiveException;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.exception.GuideNotOnDutyException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import com.ebiz.wsb.global.service.ImageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    @Value("${cloud.aws.s3.reviewImageBucketName}")
    private String reviewImageBucketName;
    private final GroupRepository groupRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final SimpMessagingTemplate template;
    private final PushNotificationService pushNotificationService;
    private final WaypointRepository waypointRepository;
    private final ImageService imageService;


    @Transactional()
    public GroupDTO startGuide() {
        Guardian guardian = getAuthenticatedGuardian();

        Group group = getGroupByGuardian(guardian);

        // 다른 인솔자가 이미 출근을 시작한 경우 예외 발생
        if (group.getIsGuideActive()) {
            throw new GroupAlreadyActiveException("해당 그룹은 이미 운행 중입니다");
        }

        // 출근 정보 업데이트
        Group updateGroup = group.toBuilder()
                .isGuideActive(true)
                .dutyGuardianId(guardian.getId())
                .shuttleStatus(false)
                .build();

        Group save = groupRepository.save(updateGroup);
        // groupRepository.flush();

        // 웹소캣으로 보낼 GroupDTO 정보 생성
        GroupDTO groupDTO = GroupDTO.builder()
                        .messageType(AttendanceMessageType.GUIDE_STATUS_CHANGE)
                        .isGuideActive(save.getIsGuideActive())
                        .dutyGuardianId(save.getDutyGuardianId())
                        .shuttleStatus(save.getShuttleStatus())
                        .build();

        Map<String, String> parentPushData = pushNotificationService.createPushData(PushType.START_WORK_PARENT);
        Map<String, String> guardianPushData = pushNotificationService.createPushData(PushType.START_WORK_GUARDIAN);

        // 지도사한테 보내는 메시지 body 값 수정
        String bodyWithGuardianName = String.format(guardianPushData.get("body"), guardian.getName());
        guardianPushData.put("body", bodyWithGuardianName);

        pushNotificationService.sendStartGuidePushNotificationToGroupDifferentMessage(group.getId(), parentPushData.get("title"), parentPushData.get("body"), guardianPushData.get("title"), guardianPushData.get("body"), PushType.START_WORK_PARENT, PushType.START_WORK_GUARDIAN);

        template.convertAndSend("/sub/group/" + group.getId(), groupDTO);

        // 업데이트된 그룹 정보를 DTO로 변환하여 반환
        return GroupDTO.builder()
                .id(save.getId())
                .groupName(save.getGroupName())
                .schoolName(save.getSchoolName())
                .isGuideActive(save.getIsGuideActive())
                .dutyGuardianId(save.getDutyGuardianId())
                .shuttleStatus(save.getShuttleStatus())
                .build();
    }

    @Transactional
    public GroupDTO stopGuide() {
        Guardian guardian = getAuthenticatedGuardian();

        // 인솔자가 속한 그룹 정보 가져오기
        Group group = getGroupByGuardian(guardian);

        // 해당 그룹의 마지막 경유지 true 값으로 변경 후 저장하기
        List<Waypoint> waypoints = group.getWaypoints();
        Waypoint waypoint = waypoints.get(waypoints.size() - 1);
        Waypoint updateWaypoint = waypoint.toBuilder()
                .attendanceComplete(true)
                .build();
        waypointRepository.save(updateWaypoint);

        // 현재 출근 상태인지, 그리고 출근한 인솔자가 요청한 인솔자와 일치하는지 확인
        if (!group.getIsGuideActive() || !guardian.getId().equals(group.getDutyGuardianId())) {
            throw new GuideNotOnDutyException("해당 지도사는 퇴근하기의 권한이 없습니다");
        }

        // 출근 상태를 해제하고 dutyGuardianId를 null로 설정
        Group updateGroup = group.toBuilder()
                .isGuideActive(false)
                .dutyGuardianId(null)
                .shuttleStatus(true)
                .build();

        Group save = groupRepository.save(updateGroup);
        // groupRepository.flush();

        // 웹소캣으로 보낼 GroupDTO와 WaypointDTO 정보 생성
        GroupDTO groupDTO = GroupDTO.builder()
                .messageType(AttendanceMessageType.GUIDE_STATUS_CHANGE)
                .isGuideActive(save.getIsGuideActive())
                .dutyGuardianId(save.getDutyGuardianId())
                .shuttleStatus(save.getShuttleStatus())
                .build();

        Map<String, String> parentPushData = pushNotificationService.createPushData(PushType.END_WORK_PARENT);
        Map<String, String> guardianPushData = pushNotificationService.createPushData(PushType.END_WORK_GUARDIAN);

        LocalTime nowInKorea = LocalTime.now();
        // 부모님한테 보내는 메시지 body 값 수정
        String bodyWithTimeAndSchoolName = String.format(parentPushData.get("body"), nowInKorea.getHour(), nowInKorea.getMinute(), group.getSchoolName());
        parentPushData.put("body", bodyWithTimeAndSchoolName);

        // 지도사한테 보내는 메시지 body 값 수정
        String bodyWithGuardianName = String.format(guardianPushData.get("body"), guardian.getName());
        guardianPushData.put("body", bodyWithGuardianName);

        // 알림센터에서 학부모가 받는 body 값 수정
        LocalDateTime now = LocalDateTime.now();
        String alarmBodyWithTime = String.format(parentPushData.get("parent_alarm_center_body"), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour(), now.getMinute(), group.getSchoolName());
        parentPushData.put("parent_alarm_center_body", alarmBodyWithTime);

        pushNotificationService.sendStopGuidePushNotificationToGroupDifferentMessage(group.getId(), parentPushData.get("title"), parentPushData.get("body"), parentPushData.get("parent_alarm_center_title"), parentPushData.get("parent_alarm_center_body"), guardianPushData.get("title"), guardianPushData.get("body"), PushType.END_WORK_PARENT, PushType.END_WORK_GUARDIAN);

        template.convertAndSend("/sub/group/" + group.getId(), groupDTO);

        // 업데이트된 그룹 정보를 DTO로 반환
        return GroupDTO.builder()
                .id(save.getId())
                .groupName(save.getGroupName())
                .schoolName(save.getSchoolName())
                .isGuideActive(save.getIsGuideActive())
                .dutyGuardianId(save.getDutyGuardianId())
                .shuttleStatus(save.getShuttleStatus())
                .build();
    }

    @Transactional
    public GroupDTO getGuideStatus() {
        // 현재 사용자 정보를 가져와 인솔자인지 확인
        Guardian guardian = getAuthenticatedGuardian();

        Group group = getGroupByGuardian(guardian);

        return GroupDTO.builder()
                .isGuideActive(group.getIsGuideActive())
                .dutyGuardianId(group.getDutyGuardianId())
                .shuttleStatus(group.getShuttleStatus())
                .build();
    }

    private Guardian getAuthenticatedGuardian() {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (!(userByContextHolder instanceof Guardian)) {
            throw new GuardianNotFoundException("해당 지도사를 찾을 수 없습니다");
        }
        return (Guardian) userByContextHolder;
    }

    private Group getGroupByGuardian(Guardian guardian) {
        return groupRepository.findById(guardian.getGroup().getId())
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다"));
    }

    // 임의로 사진 넣는 거라 별 기능 x
    public void updateStudentImage(MultipartFile imageFile, Long groupId) {

        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다."));

        String photoUrl = imageService.uploadImage(imageFile, reviewImageBucketName);
        Group updateGroup = existingGroup.toBuilder()
                .groupImage(photoUrl)
                .build();

        groupRepository.save(updateGroup);
    }
}
