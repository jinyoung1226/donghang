package com.ebiz.wsb.domain.guardian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GuardianSummaryDTO {
    private Long id;
    private String name;
    private String imagePath;
}
