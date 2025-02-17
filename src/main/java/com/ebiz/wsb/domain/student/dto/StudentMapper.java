package com.ebiz.wsb.domain.student.dto;

import com.ebiz.wsb.domain.student.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentDTO toDTO(Student student, boolean includeGroupAndWaypoint) {
        StudentDTO.StudentDTOBuilder builder = StudentDTO.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .schoolName(student.getSchoolName())
                .grade(student.getGrade())
                .notes(student.getNotes())
                .imagePath(student.getImagePath())
                .parentId(student.getParent().getId())
                .parentPhone(student.getParentPhone());

        if (includeGroupAndWaypoint) {
            builder.groupId(student.getGroup() != null ? student.getGroup().getId() : null)
                    .groupName(student.getGroup() != null ? student.getGroup().getGroupName() : null)
                    .waypointId(student.getWaypoint() != null ? student.getWaypoint().getId() : null)
                    .waypointName(student.getWaypoint() != null ? student.getWaypoint().getWaypointName() : null);
        }

        return builder.build();
    }

    public StudentDTO toDTOWithGroupAndWaypoint(Student student) {
        return StudentDTO.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .schoolName(student.getSchoolName())
                .grade(student.getGrade())
                .notes(student.getNotes())
                .imagePath(student.getImagePath())
                .groupId(student.getGroup() != null ? student.getGroup().getId() : null)
                .groupName(student.getGroup() != null ? student.getGroup().getGroupName() : null)
                .waypointId(student.getWaypoint() != null ? student.getWaypoint().getId() : null)
                .waypointName(student.getWaypoint() != null ? student.getWaypoint().getWaypointName() : null)
                .parentId(student.getParent().getId())
                .parentPhone(student.getParent().getPhone())
                .build();
    }

    public StudentDTO toDTOWithoutGroupAndWaypoint(Student student) {
        return StudentDTO.builder()
                .studentId(student.getStudentId())
                .name(student.getName())
                .schoolName(student.getSchoolName())
                .grade(student.getGrade())
                .notes(student.getNotes())
                .imagePath(student.getImagePath())
                .parentId(student.getParent().getId())
                .parentPhone(student.getParent().getPhone())
                .build();
    }
}
