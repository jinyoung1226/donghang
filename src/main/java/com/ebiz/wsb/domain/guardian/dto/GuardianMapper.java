package com.ebiz.wsb.domain.guardian.dto;

import com.ebiz.wsb.domain.guardian.entity.Guardian;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GuardianMapper {
    public GuardianDTO convertToGuardianDTO(Guardian guardian) {
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
}
