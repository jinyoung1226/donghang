package com.ebiz.wsb.domain.parent.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.parent.dto.ParentDTO;
import com.ebiz.wsb.domain.parent.dto.ParentMapper;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.parent.exception.ParentNotFoundException;
import com.ebiz.wsb.domain.parent.repository.ParentRepository;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.S3Uploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentService {

    @Value("${cloud.aws.s3.Object.parent}")
    private String ParentDirName;
    private final S3Uploader s3Uploader;
    private final ParentRepository parentRepository;
    private final AuthorizationHelper authorizationHelper;
    private final UserDetailsServiceImpl userDetailsService;
    private final ParentMapper parentMapper;

    /**
     * 학부모 본인을 조회하는 메서드
     * @return 학부모(본인)에 대한 정보를 DTO로 반환
     */
    @Transactional
    public ParentDTO getParent() {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();
        if (loggedInParent == null) {
            throw new ParentNotFoundException("학부모 정보를 찾을 수 없습니다.");
        }
        return parentMapper.convertToParentDTO(loggedInParent);
    }

    /**
     *
     * @param parentDTO 학부모 수정할 정보 DTO로 받음
     * @param imageFile 학부모 업데이트할 사진
     * @return 수정된 정보 DTO로 반환
     */
    @Transactional
    public ParentDTO updateParent(ParentDTO parentDTO, MultipartFile imageFile) {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();

        String imagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imagePath = s3Uploader.uploadImage(imageFile, ParentDirName);
        }

        Parent updatedParent = parentMapper.fromDTOToParent(parentDTO, loggedInParent, imagePath);
        return parentMapper.convertToParentDTO(parentRepository.save(updatedParent));
    }

    /**
     * 학부모 유저 삭제하는 메서드
     */
    @Transactional
    public void deleteParent() {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();
        parentRepository.deleteById(loggedInParent.getId());
    }

    /**
     * 학부모 그룹 탭에서 자신이 속한 그룹 정보를 확인하는 메서드
     * @return 학부모가 속한 그룹 DTO 반환
     */
    public GroupDTO getParentGroup() {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();
        Group group = loggedInParent.getGroup();

        if (group == null) {
            throw new GroupNotFoundException("그룹을 찾을 수 없습니다.");
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

    /**
     * 학부모가 운행 탭 들어갈 때, 운행 여부에 대해 알 수 있는 메서드
     * @return
     */
    public GroupDTO getShuttleStatus() {
        Parent loggedInParent = authorizationHelper.getLoggedInParent();
        Group group = loggedInParent.getGroup();

        return GroupDTO.builder()
                .isGuideActive(group.getIsGuideActive())
                .build();
    }
}

