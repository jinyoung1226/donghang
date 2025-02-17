package com.ebiz.wsb.domain.waypoint.repository;

import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WaypointRepository extends JpaRepository<Waypoint, Long> {
   List<Waypoint> findByGroup_Id(Long groupId);

    // 경유지 ID로 학생 목록을 조회하는 JPQL 쿼리
    @Query("SELECT s FROM Student s WHERE s.waypoint.id = :waypointId")
    List<Student> findStudentsByWaypointId(@Param("waypointId") Long waypointId);

    // 경유지의 currentCount와 attendanceComplete 필드를 초기화하는 쿼리
    @Modifying
    @Transactional
    @Query("UPDATE Waypoint w SET w.currentCount = 0, w.attendanceComplete = false")
    void resetTwoWaypointFields();
}
