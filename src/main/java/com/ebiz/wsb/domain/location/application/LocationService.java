package com.ebiz.wsb.domain.location.application;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.location.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final SimpMessagingTemplate template;
    private final GroupRepository groupRepository;

    /**
     * 위치 데이터를 서버로 전달받아, 각 그룹 별 채널에 위치 데이터 전달하는 웹소캣 메서드
     * @param locationDTO 지도사의 위도와 경도 데이터 받는 DTO
     * @param groupId 지도사가 속한 groupId 판별을 통해 해당 그룹 Id 기반으로 채널을 구독한 학부모에게 위치 데이터 전달
     */
    public void receiveAndSendLocation(LocationDTO locationDTO, Long groupId) {
        // 해당 그룹의 학부모들에게 위치 정보 전송
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("그룹 정보를 찾을 수 없습니다"));

        // 해당 그룹의 인솔자가 현재 운행 중이고(duty 상태) 담당 인솔자 ID가 설정되어 있을 때만 위치 전송
        if (group.getIsGuideActive() && group.getDutyGuardianId() != null) {
            template.convertAndSend("/sub/group/" + group.getId() + "/location", locationDTO);
        }
    }
}
