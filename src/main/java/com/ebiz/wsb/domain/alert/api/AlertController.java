package com.ebiz.wsb.domain.alert.api;

import com.ebiz.wsb.domain.alert.application.AlertService;
import com.ebiz.wsb.domain.alert.dto.AlertDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlerts(){
        List<AlertDTO> alertsDTO = alertService.getAlerts();
        return ResponseEntity.ok(alertsDTO);
    }
}
