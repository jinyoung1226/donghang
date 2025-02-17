package com.ebiz.wsb.domain.attendance.repository;

import com.ebiz.wsb.domain.attendance.entity.Attendance;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findByStudentAndAttendanceDate(Student student, LocalDate attendanceDate);

    List<Attendance> findByWaypoint_IdAndAttendanceDate(Long waypointId, LocalDate attendanceDate);

    @Transactional
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.attendanceDate IN :dates")
    void deleteByAttendanceDateIn(@Param("dates") List<LocalDate> dates);
}
