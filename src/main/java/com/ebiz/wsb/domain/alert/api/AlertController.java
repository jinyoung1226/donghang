package com.ebiz.wsb.domain.alert.api;

import com.ebiz.wsb.domain.alert.application.AlertService;
import com.ebiz.wsb.domain.alert.dto.AlertDTO;
import com.ebiz.wsb.domain.alert.entity.Alert;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<List<AlertDTO>> getAlertsForCurrentUser(){
        List<AlertDTO> alertsDTO = alertService.getAlertsForCurrentUser();
        return ResponseEntity.ok(alertsDTO);
    }
}
