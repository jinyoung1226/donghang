package com.ebiz.wsb.domain.guardian.dto;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GuardianMapper {
    public GuardianDTO toDTO(Guardian guardian) {
        return GuardianDTO.builder()
                .id(guardian.getId())
                .email(guardian.getEmail())
                .name(guardian.getName())
                .phone(guardian.getPhone())
                .bio(guardian.getBio())
                .experience(guardian.getExperience())
                .imagePath(guardian.getImagePath())
                .groupId(guardian.getGroup() != null ? guardian.getGroup().getId() : null)
                .build();
    }

    public Guardian fromDTO(GuardianDTO dto, Guardian existingGuardian, String imagePath, Group group) {
        return Guardian.builder()
                .id(existingGuardian.getId())
                .name(dto.getName() != null ? dto.getName() : existingGuardian.getName())
                .phone(dto.getPhone() != null ? dto.getPhone() : existingGuardian.getPhone()) // phone 확인
                .bio(dto.getBio() != null ? dto.getBio() : existingGuardian.getBio())
                .experience(dto.getExperience() != null ? dto.getExperience() : existingGuardian.getExperience())
                .email(existingGuardian.getEmail()) // email은 수정하지 않음
                .password(existingGuardian.getPassword()) // password는 수정하지 않음
                .imagePath(imagePath != null ? imagePath : existingGuardian.getImagePath()) // imagePath 확인
                .group(group != null ? group : existingGuardian.getGroup())
                .build();
    }



}
