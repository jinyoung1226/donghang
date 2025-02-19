package com.ebiz.wsb.domain.group.application;

import com.ebiz.wsb.domain.attendance.entity.AttendanceMessageType;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupAlreadyActiveException;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.exception.GuideNotOnDutyException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import com.ebiz.wsb.domain.waypoint.repository.WaypointRepository;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.S3Uploader;
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

    @Value("${cloud.aws.s3.Object.group}")
    private String GroupDirName;
    private final AuthorizationHelper authorizationHelper;
    private final S3Uploader s3Uploader;
    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate template;
    private final PushNotificationService pushNotificationService;
    private final WaypointRepository waypointRepository;


    /**
     * 지도사가 출근하기 누를 때 처리하는 메서드
     * @return GroupDTO
     */
    @Transactional()
    public GroupDTO startGuide() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = groupRepository.findById(loggedInGuardian.getGroup().getId())
                .orElseThrow(() -> new GroupNotFoundException("그룹 정보를 찾을 수 없습니다."));

        // 다른 인솔자가 이미 출근을 시작한 경우 예외 발생
        if (group.getIsGuideActive()) {
            throw new GroupAlreadyActiveException("이미 운행 중인 그룹입니다.");
        }

        // 출근 정보 업데이트
        Group updateGroup = group.toBuilder()
                .isGuideActive(true)
                .dutyGuardianId(loggedInGuardian.getId())
                .shuttleStatus(false)
                .build();

        Group save = groupRepository.save(updateGroup);

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
        String bodyWithGuardianName = String.format(guardianPushData.get("body"), loggedInGuardian.getName());
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


    /**
     * 지도사가 퇴근하기를 누를 때 처리하는 메서드
     * @return GroupDTO
     */
    @Transactional
    public GroupDTO stopGuide() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = groupRepository.findById(loggedInGuardian.getGroup().getId())
                .orElseThrow(() -> new GroupNotFoundException("그룹 정보를 찾을 수 없습니다."));

        // 해당 그룹의 마지막 경유지 true 값으로 변경 후 저장하기
        List<Waypoint> waypoints = group.getWaypoints();
        Waypoint waypoint = waypoints.get(waypoints.size() - 1);
        Waypoint updateWaypoint = waypoint.toBuilder()
                .attendanceComplete(true)
                .build();

        waypointRepository.save(updateWaypoint);

        // 현재 출근 상태인지, 그리고 출근한 인솔자가 요청한 인솔자와 일치하는지 확인
        if (!group.getIsGuideActive() || !loggedInGuardian.getId().equals(group.getDutyGuardianId())) {
            throw new GuideNotOnDutyException("해당 지도사는 퇴근하기 권한이 없습니다.");
        }

        // 출근 상태를 해제하고 dutyGuardianId를 null로 설정
        Group updateGroup = group.toBuilder()
                .isGuideActive(false)
                .dutyGuardianId(null)
                .shuttleStatus(true)
                .build();

        Group save = groupRepository.save(updateGroup);

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
        // 학부모한테 보내는 메시지 body 값 수정
        String bodyWithTimeAndSchoolName = String.format(parentPushData.get("body"), nowInKorea.getHour(), nowInKorea.getMinute(), group.getSchoolName());
        parentPushData.put("body", bodyWithTimeAndSchoolName);

        // 지도사한테 보내는 메시지 body 값 수정
        String bodyWithGuardianName = String.format(guardianPushData.get("body"), loggedInGuardian.getName());
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

    /**
     * 지도사가 현재 운행 상태와 대표 인솔자가 누구인지 조회하는 메서드
     * @return 지도사가 속한 그룹의 정보 DTO
     */
    @Transactional
    public GroupDTO getGuideStatus() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = groupRepository.findById(loggedInGuardian.getGroup().getId())
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다."));

        return GroupDTO.builder()
                .isGuideActive(group.getIsGuideActive())
                .dutyGuardianId(group.getDutyGuardianId())
                .shuttleStatus(group.getShuttleStatus())
                .build();
    }

    /**
     * 그룹 이미지를 업데이트 하는 메서드
     * @param imageFile 그룹 이미지 파일
     * @param groupId 그룹 Id
     */
    public void updateGroupImage(MultipartFile imageFile, Long groupId) {
        Group existingGroup = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("그룹 정보를 찾을 수 없습니다."));

        String photoUrl = s3Uploader.uploadImage(imageFile, GroupDirName);
        Group updateGroup = existingGroup.toBuilder()
                .groupImage(photoUrl)
                .build();

        // ******** 이 파트 사용자 인증 코드 넣어야 함 ********* //

        groupRepository.save(updateGroup);
    }
}
