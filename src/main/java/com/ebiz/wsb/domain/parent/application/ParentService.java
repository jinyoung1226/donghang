package com.ebiz.wsb.domain.parent.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.ebiz.wsb.domain.parent.dto.ParentMapper;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentAccessException;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import com.ebiz.wsb.domain.student.dto.StudentDTO;
import com.ebiz.wsb.domain.student.entity.Student;
import com.ebiz.wsb.domain.student.exception.ImageUploadException;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.ImageService;
import com.ebiz.wsb.global.service.S3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentService {

    @Value("${cloud.aws.s3.reviewImageBucketName}")
    private String reviewImageBucketName;
    private final ParentRepository parentRepository;
    private final AuthorizationHelper authorizationHelper;
    private final UserDetailsServiceImpl userDetailsService;
    private final ParentMapper parentMapper;
    private final ImageService imageService;

    @Transactional
    public ParentDTO getMyParentInfo() {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();

        if (loggedInParent == null) {
            throw new ParentNotFoundException("학부모 정보를 찾을 수 없습니다.");
        }

        return parentMapper.toDTO(loggedInParent);
    }



    @Transactional
    public ParentDTO updateParent(ParentDTO parentDTO, MultipartFile imageFile) {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();

        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imagePath = imageService.uploadImage(imageFile, reviewImageBucketName);
        }

        Parent updatedParent = parentMapper.fromDTO(parentDTO, loggedInParent, imagePath);

        return parentMapper.toDTO(parentRepository.save(updatedParent));
    }


    @Transactional
    public void deleteParent(Long parentId) {
        Parent existingParent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ParentNotFoundException("부모 정보를 찾을 수 없습니다."));

        authorizationHelper.validateParentAccess(existingParent, parentId);

        parentRepository.deleteById(parentId);
    }

    public GroupDTO getParentGroup() {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if (!(userByContextHolder instanceof Parent)) {
            throw new ParentNotFoundException("학부모 정보를 찾을 수 없습니다.");
        }

        Parent parent = (Parent) userByContextHolder;

        Group group = parent.getGroup();
        if (group == null) {
            throw new GroupNotFoundException("배정된 그룹을 찾을 수 없습니다.");
        }

        // 학부모 그룹 탭에서 자신의 그룹에 있는 인솔자 정보 확인하기 위해 DTO 생성
        List<GuardianDTO> guardianDTOs = group.getGuardians().stream()
                .map(guardian -> GuardianDTO.builder()
                        .name(guardian.getName())
                        .phone(guardian.getPhone())
                        .imagePath(guardian.getImagePath())
                        .build())
                .collect(Collectors.toList());

        // 그룹 내의 학생 수
        int studentCount = group.getStudents().size();

        return GroupDTO.builder()
                .groupName(group.getGroupName())
                .schoolName(group.getSchoolName())
                .dutyGuardianId(group.getDutyGuardianId())
                .id(group.getId())
                .guardians(guardianDTOs)
                .studentCount(studentCount)
                .groupImage(group.getGroupImage())
                .regionName(group.getRegionName())
                .districtName(group.getDistrictName())
                .build();
    }

    // 학부모가 운행 탭 들어갈 때, 운행 여부에 대해 알 수 있는 api
    public GroupDTO getShuttleStatus() {
        Object userByContextHolder = userDetailsService.getUserByContextHolder();
        if(userByContextHolder instanceof Parent) {
            Parent parent = (Parent) userByContextHolder;

            Group group = parent.getGroup();

            GroupDTO groupDTO = GroupDTO.builder()
                    .isGuideActive(group.getIsGuideActive())
                    .build();

            return groupDTO;
        } else {
            throw new ParentNotFoundException("해당 학부모를 찾을 수 없습니다.");
        }
    }

}

