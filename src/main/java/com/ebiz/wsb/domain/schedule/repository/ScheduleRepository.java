package com.ebiz.wsb.domain.schedule.repository;

import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE s.group.id = :groupId AND s.day BETWEEN :startDate AND :endDate")
    List<Schedule> findByGroupIdAndDayBetween(@Param("groupId") Long groupId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM Schedule s WHERE s.group.id IN :groupIds AND s.day BETWEEN :startDate AND :endDate")
    List<Schedule> findByGroupIdInAndDayBetween(@Param("groupIds") List<Long> groupIds,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
}