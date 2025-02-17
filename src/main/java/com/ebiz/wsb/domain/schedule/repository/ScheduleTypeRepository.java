package com.ebiz.wsb.domain.schedule.repository;

import com.ebiz.wsb.domain.schedule.entity.ScheduleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTypeRepository extends JpaRepository<ScheduleType, Long> {
    Optional<ScheduleType> findByName(String name);
}
