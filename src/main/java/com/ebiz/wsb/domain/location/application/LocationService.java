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

    public void receiveAndSendLocation(LocationDTO locationDTO, Long groupId) {
        // 해당 그룹의 학부모들에게 위치 정보 전송
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("해당 그룹을 찾을 수 없습니다"));

        if (group.getIsGuideActive() && group.getDutyGuardianId() != null) {
            template.convertAndSend("/sub/group/" + group.getId() + "/location", locationDTO);
        }
    }
}
