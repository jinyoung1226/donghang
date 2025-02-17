package com.ebiz.wsb.global.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@RestController
public class TimeController {
    @GetMapping("/current-time")
    public String getCurrentTime() {
        LocalDateTime localDateTime = LocalDateTime.now();
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.systemDefault());

        return String.format(
                "LocalDateTime: %s\nZonedDateTime: %s\nSystem Default Zone: %s",
                localDateTime,
                zonedDateTime,
                ZoneId.systemDefault()
        );
    }
}
