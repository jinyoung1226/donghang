package com.ebiz.wsb.domain.parent.dto;

import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParentDTO {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private GroupDTO group;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String imagePath;

    private List<StudentDTO> students;
}
