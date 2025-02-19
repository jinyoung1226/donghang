package com.ebiz.wsb.domain.location.api;

import com.ebiz.wsb.domain.location.application.LocationService;
import com.ebiz.wsb.domain.location.dto.LocationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @MessageMapping("/group/{groupId}/location")
    public void receiveAndSendLocation(@Payload LocationDTO locationDTO, @DestinationVariable Long groupId) {
        locationService.receiveAndSendLocation(locationDTO, groupId);
    }
}
