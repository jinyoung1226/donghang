package com.ebiz.wsb.domain.guardian.application;

import com.ebiz.wsb.domain.group.dto.GroupDTO;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.dto.GuardianDTO;
import com.ebiz.wsb.domain.guardian.dto.GuardianMapper;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.repository.GuardianRepository;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.S3Uploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


@Service
@RequiredArgsConstructor
@Slf4j

public class GuardianService {

    @Value("${cloud.aws.s3.Object.guardian}")
    private String GuardianDirName;
    private final S3Uploader s3Uploader;
    private final GuardianRepository guardianRepository;
    private final AuthorizationHelper authorizationHelper;
    private final GuardianMapper guardianMapper;

    /**
     * 지도사 본인을 조회하는 메서드
     * @return 지도사(본인)에 대한 정보를 DTO로 반환
     */
    public GuardianDTO getGuardian() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        return guardianMapper.convertToGuardianDTO(loggedInGuardian);
    }

    /**
     * 지도사 이미지를 수정하는 메서드
     * @param imageFile 지도사 이미지 file
     */
    @Transactional
    public void updateGuardian(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        String photoUrl = uploadImage(imageFile);

        Guardian updatedGuardian = loggedInGuardian.toBuilder()
                .imagePath(photoUrl)
                .build();

        guardianRepository.save(updatedGuardian);
    }

    /**
     * 지도사 정보를 삭제하는 메서드
     */
    @Transactional
    public void deleteGuardian() {
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


    /**
     * 지도사가 속한 그룹 정보를 가져오는 메서드
     * @return
     */
    public GroupDTO getGuardianGroup() {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = loggedInGuardian.getGroup();

        if (group == null) {
            throw new GroupNotFoundException("배정된 그룹이 없습니다.");
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
}
