package com.ebiz.wsb.domain.notice.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.group.repository.GroupRepository;
import com.ebiz.wsb.domain.guardian.dto.GuardianSummaryDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.guardian.exception.FileUploadException;
import com.ebiz.wsb.domain.notice.dto.GroupNoticeDTO;
import com.ebiz.wsb.domain.notice.dto.LikesResponseDTO;
import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import com.ebiz.wsb.domain.notice.entity.GroupNoticePhoto;
import com.ebiz.wsb.domain.notice.entity.Likes;
import com.ebiz.wsb.domain.notice.exception.LikesNumberException;
import com.ebiz.wsb.domain.notice.exception.NotNoticeInGroupException;
import com.ebiz.wsb.domain.notice.exception.NoticeAccessDeniedException;
import com.ebiz.wsb.domain.notice.exception.NoticeNotFoundException;
import com.ebiz.wsb.domain.notice.repository.GroupNoticeRepository;
import com.ebiz.wsb.domain.notice.repository.LikesRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.entity.Parent;
import com.ebiz.wsb.global.service.S3Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ebiz.wsb.domain.notice.entity.QGroupNotice.groupNotice;

@Service
@RequiredArgsConstructor
@Slf4j

public class GroupNoticeService {

    @Value("${cloud.aws.s3.reviewImageBucketName}")
    private String reviewImageBucketName;
    private final GroupNoticeRepository groupNoticeRepository;
    private final S3Service s3service;
    private final UserDetailsServiceImpl userDetailsService;
    private final GroupRepository groupRepository;
    private final PushNotificationService pushNotificationService;
    private final LikesRepository likesRepository;

    @Transactional
    public Page<GroupNoticeDTO> getAllGroupNotices(Pageable pageable) {
        Long groupId = getCurrentGroupId();
        Page<GroupNotice> notices = groupNoticeRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        if (notices.isEmpty()) {
            log.info("그룹 ID {}에 대한 공지사항이 없습니다.", groupId);
            return Page.empty();
        }

        return notices.map(this::convertToDTO);
    }


    @Transactional
    public GroupNoticeDTO getGroupNoticeByGroupNoticeId(Long groupNoticeId) {

        Object currentUser = userDetailsService.getUserByContextHolder();
        Long groupId;

        if (currentUser instanceof Guardian) {
            Guardian guardian = (Guardian) currentUser;

            if (guardian.getGroup() == null || guardian.getGroup().getId() == null) {
                log.error("지도사 ID {}는 그룹에 속해 있지 않습니다.", guardian.getId());
                throw new NoticeAccessDeniedException("해당 지도사는 그룹에 속해 있지 않습니다.");
            }

            groupId = guardian.getGroup().getId();

        } else if (currentUser instanceof Parent) {
            Parent parent = (Parent) currentUser;

            if (parent.getGroup() == null || parent.getGroup().getId() == null) {
                log.error("부모 ID {}는 그룹에 속해 있지 않습니다.", parent.getId());
                throw new NoticeAccessDeniedException("해당 학부모는 그룹에 속해 있지 않습니다.");
            }

            groupId = parent.getGroup().getId();
        } else {
            throw new NoticeAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        if (!groupNotice.getGroup().getId().equals(groupId)) {
            throw new NoticeAccessDeniedException("해당 그룹에 속하지 않은 공지사항입니다.");
        }

        return convertToDTO(groupNotice);
    }


    public GroupNoticeDTO createGroupNotice(String content, List<MultipartFile> imageFiles) {
        Object currentUser = userDetailsService.getUserByContextHolder();

        if (!(currentUser instanceof Guardian guardian)) {
            log.error("부모 계정 {}로 공지사항 등록 시도", currentUser);
            throw new NoticeAccessDeniedException("부모 계정으로 공지사항을 등록할 수 없습니다.");
        }

        Group group = guardian.getGroup();
        if (group == null) {
            throw new GroupNotFoundException("지도사가 그룹에 속해 있지 않습니다.");
        }

        GroupNotice groupNotice = GroupNotice.builder()
                .guardian(guardian)
                .group(group)
                .content(content)
                .likes(0)
                .createdAt(LocalDateTime.now())
                .photos(new ArrayList<>())
                .build();

        List<GroupNoticePhoto> photoEntities = new ArrayList<>();
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String photoUrl = uploadImage(file);
                    photoEntities.add(GroupNoticePhoto.builder()
                            .photoUrl(photoUrl)
                            .groupNotice(groupNotice)
                            .build());
                }
            }
        }
        groupNotice.getPhotos().addAll(photoEntities);

        groupNoticeRepository.save(groupNotice);

        Map<String, String> pushData = pushNotificationService.createPushData(PushType.POST);

        // 푸시 알림 body 내용에 인솔자 이름 삽입
        String bodyWithGuardianName = String.format(pushData.get("body"), guardian.getName());
        pushData.put("body", bodyWithGuardianName);

        // 알림센터 body 내용에 공지사항 글 삽입
        String alertBodyWithContent = String.format(pushData.get("parent_alarm_center_body"), content);
        pushData.put("parent_alarm_center_body", alertBodyWithContent);

        pushNotificationService.sendPushNotificationToParents(group.getId(), pushData.get("title"), pushData.get("body"), pushData.get("parent_alarm_center_title"), pushData.get("parent_alarm_center_body"), PushType.POST);

        return convertToDTO(groupNotice);
    }


    @Transactional
    public GroupNoticeDTO updateGroupNotice(Long groupNoticeId, String content, List<MultipartFile> imageFiles) {
        Guardian currentGuardian = (Guardian) userDetailsService.getUserByContextHolder();

        return groupNoticeRepository.findById(groupNoticeId)
                .map(existingGroupNotice -> {
                    if (!existingGroupNotice.getGuardian().getId().equals(currentGuardian.getId())) {
                        throw new NoticeAccessDeniedException("공지사항을 수정할 권한이 없습니다.");
                    }

                    List<GroupNoticePhoto> updatedPhotoEntities = new ArrayList<>();

                    if (imageFiles != null && !imageFiles.isEmpty()) {
                        for (MultipartFile file : imageFiles) {
                            if (!file.isEmpty()) {
                                String photoUrl = uploadImage(file);
                                updatedPhotoEntities.add(GroupNoticePhoto.builder()
                                        .photoUrl(photoUrl)
                                        .groupNotice(existingGroupNotice)
                                        .build());
                            }
                        }
                    }

                    GroupNotice updatedGroupNotice = GroupNotice.builder()
                            .groupNoticeId(existingGroupNotice.getGroupNoticeId())
                            .guardian(existingGroupNotice.getGuardian())
                            .group(existingGroupNotice.getGroup())
                            .content(content)
                            .photos(updatedPhotoEntities)
                            .likes(existingGroupNotice.getLikes())
                            .createdAt(existingGroupNotice.getCreatedAt())
                            .build();

                    GroupNotice savedGroupNotice = groupNoticeRepository.save(updatedGroupNotice);
                    return convertToDTO(savedGroupNotice);
                })
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));
    }

    public void deleteGroupNotice(Long groupNoticeId) {
        Guardian currentGuardian = (Guardian) userDetailsService.getUserByContextHolder();

        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        if (!groupNotice.getGuardian().getId().equals(currentGuardian.getId())) {
            throw new NoticeAccessDeniedException("공지사항을 삭제할 권한이 없습니다.");
        }

        groupNoticeRepository.deleteById(groupNoticeId);
    }

    private GroupNoticeDTO convertToDTO(GroupNotice groupNotice) {

        Object currentUser = userDetailsService.getUserByContextHolder();
        Long userId;

        if (currentUser instanceof Guardian) {
            userId = ((Guardian) currentUser).getId();
        } else if (currentUser instanceof Parent) {
            userId = ((Parent) currentUser).getId();
        } else {
            throw new NoticeAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        boolean liked = likesRepository.findByUserIdAndGroupNotice(userId, groupNotice).isPresent();

        List<String> photoUrls = groupNotice.getPhotos().stream()
                .map(GroupNoticePhoto::getPhotoUrl)
                .collect(Collectors.toList());

        Guardian guardian = groupNotice.getGuardian();
        GuardianSummaryDTO guardianSummaryDTO = GuardianSummaryDTO.builder()
                .id(guardian.getId())
                .name(guardian.getName())
                .imagePath(guardian.getImagePath())
                .build();

        return GroupNoticeDTO.builder()
                .groupNoticeId(groupNotice.getGroupNoticeId())
                .content(groupNotice.getContent())
                .photos(photoUrls)
                .likes(groupNotice.getLikes())
                .createdAt(groupNotice.getCreatedAt())
                .guardian(guardianSummaryDTO)
                .liked(liked)
                .build();
    }

    private String uploadImage(MultipartFile imageFile) {
        try {
            return s3service.uploadImageFile(imageFile, reviewImageBucketName);
        } catch (IOException e) {
            throw new FileUploadException("이미지 업로드 실패", e);
        }
    }

    @Transactional
    public LikesResponseDTO toggleLike(Long groupNoticeId) {
        Object currentUser = userDetailsService.getUserByContextHolder();
        Long userId;

        if (currentUser instanceof Guardian) {
            userId = ((Guardian) currentUser).getId();
        } else if (currentUser instanceof Parent) {
            userId = ((Parent) currentUser).getId();
        } else {
            throw new NoticeAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        Optional<Likes> existingLike = likesRepository.findByUserIdAndGroupNotice(userId, groupNotice);
        boolean liked;
        int likesCount;

        if (existingLike.isPresent()) {

            likesRepository.delete(existingLike.get());
            likesCount = groupNotice.getLikes() - 1;

            groupNotice = groupNotice.toBuilder().likes(likesCount).build();
            groupNoticeRepository.save(groupNotice);

            liked = false;
        } else {

            Likes newLike = Likes.builder()
                    .userId(userId)
                    .groupNotice(groupNotice)
                    .liked(true)
                    .build();
            likesRepository.save(newLike);

            likesCount = groupNotice.getLikes() + 1;

            groupNotice = groupNotice.toBuilder().likes(likesCount).build();
            groupNoticeRepository.save(groupNotice);

            liked = true;
        }

        return LikesResponseDTO.builder()
                .groupNoticeId(groupNoticeId)
                .liked(liked)
                .likesCount(likesCount)
                .build();
    }

    private Long getCurrentUserId() {
        Object currentUser = userDetailsService.getUserByContextHolder();
        if (currentUser instanceof Guardian) {
            return ((Guardian) currentUser).getId();
        } else if (currentUser instanceof Parent) {
            return ((Parent) currentUser).getId();
        } else {
            throw new NoticeAccessDeniedException("인증되지 않은 사용자입니다.");
        }
    }

    private Long getCurrentGroupId() {
        Object currentUser = userDetailsService.getUserByContextHolder();
        if (currentUser instanceof Guardian guardian) {
            if (guardian.getGroup() == null) {
                throw new NoticeAccessDeniedException("해당 지도사는 그룹에 속해 있지 않습니다.");
            }
            return guardian.getGroup().getId();
        } else if (currentUser instanceof Parent parent) {
            if (parent.getGroup() == null) {
                throw new NoticeAccessDeniedException("해당 학부모는 그룹에 속해 있지 않습니다.");
            }
            return parent.getGroup().getId();
        } else {
            throw new NoticeAccessDeniedException("인증되지 않은 사용자입니다.");
        }
    }

}