package com.ebiz.wsb.domain.guardian.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.dto.GuardianMapper;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.FileUploadException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotAccessException;
import com.ebiz.wsb.domain.guardian.exception.GuardianNotFoundException;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.domain.waypoint.entity.Waypoint;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class GuardianService {

    private static final String GUARDIAN_NOT_FOUND = "지도사 정보를 찾을 수 없습니다.";
    private static final String GUARDIAN_ACCESS_DENIED = "본인의 정보만 조회할 수 있습니다.";
    private static final String GROUP_NOT_FOUND = "아이의 그룹 정보를 찾을 수 없습니다.";


    @Value("${cloud.aws.s3.reviewImageBucketName}")
    private String reviewImageBucketName;
    private final GuardianRepository guardianRepository;
    private final AuthorizationHelper authorizationHelper;
    private final GuardianMapper guardianMapper;
    private final UserDetailsServiceImpl userDetailsService;
    private final ImageService imageService;
    private final S3Service s3Service;

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
        log.info("지도사 이미지 업데이트 완료 - ID: {}, 이미지 경로: {}", loggedInGuardian.getId(), photoUrl);
    }


    @Transactional
    public void deleteMyGuardianInfo() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        if (loggedInGuardian.getImagePath() != null) {
            try {
                imageService.deleteImage(loggedInGuardian.getImagePath(), reviewImageBucketName);
            } catch (Exception e) {
                log.error("이미지 삭제 실패 - 경로: {}, 에러 메시지: {}", loggedInGuardian.getImagePath(), e.getMessage());
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

    private String uploadImage(MultipartFile imageFile) {
        try {
            String photoUrl = s3Service.uploadImageFile(imageFile, reviewImageBucketName);
            log.info("이미지 업로드 성공 - 파일 이름: {}, S3 URL: {}", imageFile.getOriginalFilename(), photoUrl);
            return photoUrl;
        } catch (IOException e) {
            log.error("이미지 업로드 실패 - 파일 이름: {}, 에러 메시지: {}", imageFile.getOriginalFilename(), e.getMessage());
            throw new FileUploadException("이미지 업로드 실패", e);
        }
    }

    private Guardian getLoggedInGuardian() {
        return authorizationHelper.getLoggedInGuardian();
    }
}
