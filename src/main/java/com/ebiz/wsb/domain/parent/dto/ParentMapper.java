package com.ebiz.wsb.domain.parent.dto;

import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParentMapper {

    public ParentDTO toDTO(Parent parent) {
        return ParentDTO.builder()
                .id(parent.getId())
                .name(parent.getName())
                .phone(parent.getPhone())
                .email(parent.getEmail())
                .address(parent.getAddress())
                .imagePath(parent.getImagePath())
                .students(
                        parent.getStudents().stream()
                                .map(student -> StudentDTO.builder()
                                        .studentId(student.getStudentId())
                                        .name(student.getName())
                                        .schoolName(student.getSchoolName())
                                        .grade(student.getGrade())
                                        .notes(student.getNotes())
                                        .imagePath(student.getImagePath())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }

    public Parent fromDTO(ParentDTO parentDTO, Parent existingParent, String imagePath) {
        return Parent.builder()
                .id(existingParent.getId())
                .name(parentDTO.getName())
                .phone(parentDTO.getPhone())
                .address(parentDTO.getAddress())
                .email(existingParent.getEmail()) // 이메일은 변경하지 않음
                .imagePath(imagePath)
                .password(existingParent.getPassword()) // 패스워드는 유지
                .build();
    }
}
