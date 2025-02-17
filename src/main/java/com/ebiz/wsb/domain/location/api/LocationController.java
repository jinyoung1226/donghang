package com.ebiz.wsb.domain.location.api;

import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.location.application.LocationService;
import com.ebiz.wsb.domain.location.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController {


    private final LocationService locationService;

    // 위치 정보를 서버로 전달받고, 채널에 위치 정보를 전달
    @MessageMapping("/group/{groupId}/location")
    public void receiveAndSendLocation(@Payload LocationDTO locationDTO, @DestinationVariable Long groupId) {
        locationService.receiveAndSendLocation(locationDTO, groupId);
    }
}
