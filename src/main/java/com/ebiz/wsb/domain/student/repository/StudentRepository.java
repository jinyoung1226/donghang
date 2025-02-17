package com.ebiz.wsb.domain.student.repository;

import com.ebiz.wsb.domain.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByParentId(Long parentId);

    List<Student> findAllByGroupId(Long groupId);

    List<Student> findByWaypointId(Long waypointId);
}
