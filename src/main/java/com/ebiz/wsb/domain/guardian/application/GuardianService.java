package com.ebiz.wsb.domain.guardian.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.dto.GuardianMapper;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotAccessException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
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

public class GuardianService {

    private static final String GUARDIAN_NOT_FOUND = "지도사 정보를 찾을 수 없습니다.";
    private static final String GUARDIAN_ACCESS_DENIED = "본인의 정보만 조회할 수 있습니다.";
    private static final String GROUP_NOT_FOUND = "아이의 그룹 정보를 찾을 수 없습니다.";
    private final S3Uploader s3Uploader;


    @Value("${cloud.aws.s3.Object.guardian}")
    private String GuardianDirName;
    private final GuardianRepository guardianRepository;
    private final AuthorizationHelper authorizationHelper;
    private final GuardianMapper guardianMapper;
    private final UserDetailsServiceImpl userDetailsService;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public GuardianDTO getMyGuardianInfo() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        return guardianMapper.toDTO(loggedInGuardian);
    }

    public List<GuardianDTO> getGuardiansForMyChild() {
        Object currentUser = userDetailsService.getUserByContextHolder();

        if (!(currentUser instanceof Parent parent)) {
            throw new GuardianNotAccessException("학부모만 지도사 정보를 조회할 수 있습니다.");
        }

        List<Long> childGroupIds = parent.getStudents().stream()
                .map(student -> student.getGroup().getId())
                .distinct()
                .collect(Collectors.toList());

        if (childGroupIds.isEmpty()) {
            throw new GroupNotFoundException(GROUP_NOT_FOUND);
        }

        List<Guardian> guardians = guardianRepository.findGuardiansByGroupId(childGroupIds.get(0));

        return guardians.stream()
                .map(guardianMapper::toDTO)
                .collect(Collectors.toList());
    }

    public GuardianDTO getGuardianById(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new GuardianNotFoundException(GUARDIAN_NOT_FOUND));

        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        if (!loggedInGuardian.getId().equals(guardianId)) {
            throw new GuardianNotAccessException(GUARDIAN_ACCESS_DENIED);
        }

        return guardianMapper.toDTO(guardian);
    }

    @Transactional
    public void updateGuardianImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        Guardian loggedInGuardian = getLoggedInGuardian();
        String photoUrl = uploadImage(imageFile);

        Guardian updatedGuardian = loggedInGuardian.toBuilder()
                .imagePath(photoUrl)
                .build();

        guardianRepository.save(updatedGuardian);
    }


    @Transactional
    public void deleteMyGuardianInfo() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        if (loggedInGuardian.getImagePath() != null) {
            try {
                String imageUrl = loggedInGuardian.getImagePath();
                String fileKey = imageUrl.substring(imageUrl.indexOf("b-cube-web")); // 키만 뽑아내기
                s3Uploader.deleteFile(fileKey);
            } catch (Exception e) {
                log.error("S3 파일 삭제 실패: {}", e.getMessage());
            }
        }
        guardianRepository.deleteById(loggedInGuardian.getId());
    }

    public GroupDTO getGuardianGroup() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = loggedInGuardian.getGroup();

        if (group == null) {
            throw new GroupNotFoundException("배정된 그룹을 찾을 수 없습니다.");
        }

        return GroupDTO.builder()
                .groupName(group.getGroupName())
                .schoolName(group.getSchoolName())
                .dutyGuardianId(group.getDutyGuardianId())
                .id(group.getId())
                .build();
    }

    /**
     *
     * @param imageFile 업로드 파일
     * @return 업로드된 파일의 URL
     */
    private String uploadImage(MultipartFile imageFile) {
        return s3Uploader.uploadImage(imageFile, GuardianDirName);
    }

    private Guardian getLoggedInGuardian() {
        return authorizationHelper.getLoggedInGuardian();
    }
}
