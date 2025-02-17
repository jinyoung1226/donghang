package com.ebiz.wsb.domain.notice.api;

import com.ebiz.wsb.domain.notice.application.CommentService;
import com.ebiz.wsb.domain.notice.application.GroupNoticeService;
import com.ebiz.wsb.domain.notice.dto.CommentDTO;
import com.ebiz.wsb.domain.notice.dto.GroupNoticeDTO;
import com.ebiz.wsb.domain.notice.dto.LikesResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/group-notices")
@AllArgsConstructor
public class GroupNoticeController {

    private final GroupNoticeService groupNoticeService;
    private final CommentService commentService;
    @GetMapping("/group/notices")
    public ResponseEntity<Page<GroupNoticeDTO>> getAllGroupNotices(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GroupNoticeDTO> groupNotices = groupNoticeService.getAllGroupNotices(pageable);
        return ResponseEntity.ok(groupNotices);
    }

    @GetMapping("/group/notice/{groupNoticeId}")
    public ResponseEntity<GroupNoticeDTO> getGroupNoticeByGroupNoticeId(
            @PathVariable Long groupNoticeId
    ) {
        GroupNoticeDTO groupNotice = groupNoticeService.getGroupNoticeByGroupNoticeId(groupNoticeId);  // 수정된 메서드 호출
        return ResponseEntity.ok(groupNotice);
    }

    @PostMapping
    public ResponseEntity<GroupNoticeDTO> createGroupNotice(
            @RequestParam("content") String content,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos
    ) {
        GroupNoticeDTO createdGroupNotice = groupNoticeService.createGroupNotice(content, photos);
        return new ResponseEntity<>(createdGroupNotice, HttpStatus.CREATED);
    }

    @PutMapping("/{groupNoticeId}")
    public ResponseEntity<GroupNoticeDTO> updateGroupNotice(
            @PathVariable Long groupNoticeId,
            @RequestParam("content") String content,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos
    ) {
        GroupNoticeDTO updatedNotice = groupNoticeService.updateGroupNotice(groupNoticeId, content, photos);
        return ResponseEntity.ok(updatedNotice);
    }

    @DeleteMapping("/{groupNoticeId}")
    public ResponseEntity<Void> deleteGroupNotice(@PathVariable Long groupNoticeId) {
        groupNoticeService.deleteGroupNotice(groupNoticeId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{groupNoticeId}/like")
    public ResponseEntity<LikesResponseDTO> toggleLike(@PathVariable Long groupNoticeId) {
        LikesResponseDTO response = groupNoticeService.toggleLike(groupNoticeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{groupNoticeId}/comments")
    public ResponseEntity<CommentDTO> addComment(@PathVariable Long groupNoticeId,
                                                 @RequestParam String content,
                                                 Authentication authentication){
        CommentDTO commentDTO = commentService.addComment(groupNoticeId, content);
        return new ResponseEntity<>(commentDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{groupNoticeId}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long groupNoticeId){
        List<CommentDTO> comments = commentService.getCommentsByGroupNoticeId(groupNoticeId);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable Long commentId) {
        String message = commentService.deleteComment(commentId);
        return ResponseEntity.ok(message);
    }
}
