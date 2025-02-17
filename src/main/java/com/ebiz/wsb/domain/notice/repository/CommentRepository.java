package com.ebiz.wsb.domain.notice.repository;

import com.ebiz.wsb.domain.notice.entity.Comment;
import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByGroupNoticeOrderByCreatedAtDesc(GroupNotice groupNotice);
}
