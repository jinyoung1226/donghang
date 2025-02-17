package com.ebiz.wsb.domain.schedule.api;

import com.ebiz.wsb.domain.schedule.application.ScheduleService;
import com.ebiz.wsb.domain.schedule.dto.ScheduleDTO;

import java.time.LocalDate;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    public ResponseEntity<ScheduleDTO> createSchedule(@RequestBody ScheduleDTO scheduleDTO) {
        ScheduleDTO createdSchedule = scheduleService.createSchedule(scheduleDTO);
        return ResponseEntity.ok(createdSchedule);
    }

    @GetMapping("/day")
    public ResponseEntity<List<ScheduleDTO>> getDaySchedules(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ScheduleDTO> schedules = scheduleService.getSchedulesByRoleAndDateRange(date, date);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/month")
    public ResponseEntity<List<ScheduleDTO>> getMonthSchedules(
            @RequestParam("year") int year, @RequestParam("month") int month) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        List<ScheduleDTO> schedules = scheduleService.getSchedulesByRoleAndDateRange(startOfMonth, endOfMonth);
        return ResponseEntity.ok(schedules);
    }

    @PutMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDTO> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleDTO scheduleDTO) {
        ScheduleDTO updatedSchedule = scheduleService.updateSchedule(scheduleId, scheduleDTO);
        return ResponseEntity.ok(updatedSchedule);
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
