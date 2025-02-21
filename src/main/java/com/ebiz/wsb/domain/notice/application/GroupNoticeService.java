package com.ebiz.wsb.domain.notice.application;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.group.exception.GroupNotFoundException;
import com.ebiz.wsb.domain.guardian.dto.GuardianSummaryDTO;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.notice.dto.GroupNoticeDTO;
import com.ebiz.wsb.domain.notice.dto.LikesResponseDTO;
import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import com.ebiz.wsb.domain.notice.entity.GroupNoticePhoto;
import com.ebiz.wsb.domain.notice.entity.Likes;
import com.ebiz.wsb.domain.notice.exception.NoticeAccessDeniedException;
import com.ebiz.wsb.domain.notice.exception.NoticeNotFoundException;
import com.ebiz.wsb.domain.notice.repository.GroupNoticeRepository;
import com.ebiz.wsb.domain.notice.repository.LikesRepository;
import com.ebiz.wsb.domain.notification.application.PushNotificationService;
import com.ebiz.wsb.domain.notification.dto.PushType;
import com.ebiz.wsb.domain.parent.entity.Parent;

import com.ebiz.wsb.global.exception.InvalidUserTypeException;
import com.ebiz.wsb.global.service.AuthorizationHelper;
import com.ebiz.wsb.global.service.S3Uploader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class GroupNoticeService {

    @Value("${cloud.aws.s3.Object.groupNotice}")
    private String GroupNoticeDirName;
    private final AuthorizationHelper authorizationHelper;
    private final GroupNoticeRepository groupNoticeRepository;
    private final S3Uploader s3Uploader;
    private final PushNotificationService pushNotificationService;
    private final LikesRepository likesRepository;

    /**
     * 학부모 또는 지도사가 자신의 그룹 공지사항 전체 글을 조회하는 메서드
     * @param pageable
     * @return 글 Page DTO
     */
    @Transactional
    public Page<GroupNoticeDTO> getAllGroupNotices(Pageable pageable) {
        Object currentUser = authorizationHelper.getCurrentUser();
        Group group;

        if(currentUser instanceof Parent parent) {
            group = parent.getGroup();
        } else if (currentUser instanceof Guardian guardian) {
            group = guardian.getGroup();
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
        }
        Long groupId = group.getId();
        Page<GroupNotice> notices = groupNoticeRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId, pageable);

        return notices.isEmpty() ? Page.empty() : notices.map(notice -> convertToDTO(notice, currentUser));
    }

    /**
     * 학부모 또는 지도사가 자신의 그룹 공지사항 글 하나를 조회하는 메서드
     * @param groupNoticeId
     * @return
     */
    @Transactional
    public GroupNoticeDTO getGroupNoticeOne(Long groupNoticeId) {
        Object currentUser = authorizationHelper.getCurrentUser();
        Group group;

        if (currentUser instanceof Guardian guardian) {
            group = guardian.getGroup();
        } else if (currentUser instanceof Parent parent) {
            group = parent.getGroup();
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
        }

        if (group == null || group.getId() == null) {
            throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
        }

        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        if (!groupNotice.getGroup().getId().equals(group.getId())) {
            throw new NoticeAccessDeniedException("해당 그룹의 공지사항이 아닙니다.");
        }

        return convertToDTO(groupNotice,currentUser);
    }

    /**
     * 그룹 공지를 생성하는 메서드
     * @param content 공지사항 내용
     * @param imageFiles 공지사항 사진 파일
     * @return 생성된 그룹 공지 DTO
     */
    public GroupNoticeDTO createGroupNotice(String content, List<MultipartFile> imageFiles) {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();
        Group group = loggedInGuardian.getGroup();
        if (group == null) {
            throw new GroupNotFoundException("그룹 정보를 찾을 수 없습니다.");
        }

        GroupNotice groupNotice = GroupNotice.builder()
                .guardian(loggedInGuardian)
                .group(group)
                .content(content)
                .likes(0)
                .createdAt(LocalDateTime.now())
                .photos(new ArrayList<>())
                .build();

        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<GroupNoticePhoto> photoEntities = imageFiles.stream()
                    .filter(file -> !file.isEmpty())
                    .map(file -> GroupNoticePhoto.builder()
                            .photoUrl(s3Uploader.uploadImage(file, GroupNoticeDirName))
                            .groupNotice(groupNotice)
                            .build())
                    .collect(Collectors.toList());
            groupNotice.getPhotos().addAll(photoEntities);
        }

        groupNoticeRepository.save(groupNotice);

        Map<String, String> pushData = pushNotificationService.createPushData(PushType.POST);

        // 푸시 알림 body 내용에 인솔자 이름 삽입
        String bodyWithGuardianName = String.format(pushData.get("body"), loggedInGuardian.getName());
        pushData.put("body", bodyWithGuardianName);

        // 알림센터 body 내용에 공지사항 글 삽입
        String alertBodyWithContent = String.format(pushData.get("parent_alarm_center_body"), content);
        pushData.put("parent_alarm_center_body", alertBodyWithContent);

        pushNotificationService.sendPushNotificationToParents(group.getId(), pushData.get("title"), pushData.get("body"), pushData.get("parent_alarm_center_title"), pushData.get("parent_alarm_center_body"), PushType.POST);
        return convertToDTO(groupNotice, loggedInGuardian);
    }


    /**
     * 그룹 공지를 수정하는 메서드
     * @param groupNoticeId 수정할 그룹 공지 Id
     * @param content 수정할 그룹 공지 내용
     * @param imageFiles 수정할 그룹 공지 사진
     * @return 수정된 그룹 공지 DTO
     */
    @Transactional
    public GroupNoticeDTO updateGroupNotice(Long groupNoticeId, String content, List<MultipartFile> imageFiles) {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        GroupNotice existingGroupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        if (!existingGroupNotice.getGuardian().getId().equals(loggedInGuardian.getId())) {
            throw new NoticeAccessDeniedException("공지사항을 수정할 권한이 없습니다.");
        }

        List<GroupNoticePhoto> updatedPhotoEntities = new ArrayList<>();

        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String photoUrl = s3Uploader.uploadImage(file, GroupNoticeDirName);
                    updatedPhotoEntities.add(GroupNoticePhoto.builder()
                            .photoUrl(photoUrl)
                            .groupNotice(existingGroupNotice)
                            .build());
                }
            }
        }

        existingGroupNotice = GroupNotice.builder()
                .id(existingGroupNotice.getId())
                .guardian(existingGroupNotice.getGuardian())
                .group(existingGroupNotice.getGroup())
                .content(content)
                .photos(updatedPhotoEntities)
                .likes(existingGroupNotice.getLikes())
                .createdAt(existingGroupNotice.getCreatedAt())
                .build();

        groupNoticeRepository.save(existingGroupNotice);

        return convertToDTO(existingGroupNotice, loggedInGuardian);
    }

    /**
     * 그룹 공지를 삭제하는 메서드
     * @param groupNoticeId
     */
    public void deleteGroupNotice(Long groupNoticeId) {
        Guardian loggedInGuardian = authorizationHelper.getLoggedInGuardian();

        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        if (!groupNotice.getGuardian().getId().equals(loggedInGuardian.getId())) {
            throw new NoticeAccessDeniedException("공지사항을 삭제할 권한이 없습니다.");
        }
        groupNoticeRepository.deleteById(groupNoticeId);
    }

    /**
     * 그룹 공지에 좋아요 기능을 담당하는 메서드
     * @param groupNoticeId
     * @return
     */
    @Transactional
    public LikesResponseDTO toggleLike(Long groupNoticeId) {
        Object currentUser = authorizationHelper.getCurrentUser();
        Long userId;

        if (currentUser instanceof Guardian) {
            userId = ((Guardian) currentUser).getId();
        } else if (currentUser instanceof Parent) {
            userId = ((Parent) currentUser).getId();
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
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



    private GroupNoticeDTO convertToDTO(GroupNotice groupNotice, Object currentUser) {
        Long userId;

        if (currentUser instanceof Guardian) {
            userId = ((Guardian) currentUser).getId();
        } else if (currentUser instanceof Parent) {
            userId = ((Parent) currentUser).getId();
        } else {
            throw new InvalidUserTypeException("유효하지 않은 유저 타입입니다.");
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
                .groupNoticeId(groupNotice.getId())
                .content(groupNotice.getContent())
                .photos(photoUrls)
                .likes(groupNotice.getLikes())
                .createdAt(groupNotice.getCreatedAt())
                .guardian(guardianSummaryDTO)
                .liked(liked)
                .build();
    }
}