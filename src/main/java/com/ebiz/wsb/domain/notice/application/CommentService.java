package com.ebiz.wsb.domain.notice.application;

import com.ebiz.wsb.domain.auth.application.UserDetailsServiceImpl;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.notice.dto.CommentDTO;
import com.ebiz.wsb.domain.notice.dto.GroupNoticeDTO;
import com.ebiz.wsb.domain.notice.entity.Comment;
import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import com.ebiz.wsb.domain.notice.exception.CommentAccessDeniedException;
import com.ebiz.wsb.domain.notice.exception.CommentNotFoundException;
import com.ebiz.wsb.domain.notice.exception.NoticeNotFoundException;
import com.ebiz.wsb.domain.notice.repository.CommentRepository;
import com.ebiz.wsb.domain.notice.repository.GroupNoticeRepository;
import com.ebiz.wsb.domain.parent.entity.Parent;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final GroupNoticeRepository groupNoticeRepository;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public CommentDTO addComment(Long groupNoticeId, String content) {
        Object currentUser = userDetailsService.getUserByContextHolder();
        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        Comment comment;
        if (currentUser instanceof Guardian) {
            Guardian guardian = (Guardian) currentUser;
            comment = Comment.builder()
                    .groupNotice(groupNotice)
                    .guardian(guardian)
                    .content(content)
                    .build();
        } else if (currentUser instanceof Parent) {
            Parent parent = (Parent) currentUser;
            comment = Comment.builder()
                    .groupNotice(groupNotice)
                    .parent(parent)
                    .content(content)
                    .build();
        } else {
            throw new CommentAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        Comment savedComment = commentRepository.save(comment);
        return convertToDTO(savedComment);
    }

    public List<CommentDTO> getCommentsByGroupNoticeId(Long groupNoticeId) {
        GroupNotice groupNotice = groupNoticeRepository.findById(groupNoticeId)
                .orElseThrow(() -> new NoticeNotFoundException(groupNoticeId));

        return commentRepository.findAllByGroupNoticeOrderByCreatedAtDesc(groupNotice)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public String deleteComment(Long commentId) {
        Object currentUser = userDetailsService.getUserByContextHolder();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (currentUser instanceof Guardian) {
            Guardian guardian = (Guardian) currentUser;
            if (!comment.getGuardian().getId().equals(guardian.getId())) {
                throw new CommentAccessDeniedException("삭제할 권한이 없습니다.");
            }
        } else if (currentUser instanceof Parent) {
            Parent parent = (Parent) currentUser;
            if (!comment.getParent().getId().equals(parent.getId())) {
                throw new CommentAccessDeniedException("삭제할 권한이 없습니다.");
            }
        } else {
            throw new CommentAccessDeniedException("인증되지 않은 사용자는 게시글을 등록할 수 없습니다.");
        }

        commentRepository.delete(comment);
        return "댓글이 삭제되었습니다.";
    }


    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO.CommentDTOBuilder builder = CommentDTO.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt());

        if (comment.getGuardian() != null) {
            builder.guardianId(comment.getGuardian().getId())
                    .guardianName(comment.getGuardian().getName());
        }

        if (comment.getParent() != null) {
            builder.parentId(comment.getParent().getId())
                    .parentName(comment.getParent().getName());
        }

        return builder.build();
    }
}
