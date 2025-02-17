package com.ebiz.wsb.timezone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@SpringBootTest
public class TimezoneTest {

    @Test
    void testDefaultTimezone(){
        ZoneId defaultZoneId = ZoneId.systemDefault();
        System.out.println("기본 시간대: " + defaultZoneId);

        Assertions.assertEquals("Asia/Seoul", defaultZoneId.toString());
    }

    @Test
    void testCurrentTime(){
        ZonedDateTime now = ZonedDateTime.now();
        System.out.println("현재 시간 : " + now);

        Assertions.assertEquals("Asia/Seoul", now.getZone().toString());
    }
}
