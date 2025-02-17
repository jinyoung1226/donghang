package com.ebiz.wsb.domain.guardian.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GuardianDTO {

    private Long id;
    private String name;
    private String bio;
    private String experience;
    private String phone;
    private String email;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String imagePath;
    private Long groupId;

}
