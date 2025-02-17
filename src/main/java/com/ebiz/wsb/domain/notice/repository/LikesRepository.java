package com.ebiz.wsb.domain.notice.repository;

import com.ebiz.wsb.domain.auth.dto.UserType;
import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import com.ebiz.wsb.domain.notice.entity.Likes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikesRepository extends JpaRepository<Likes, Long> {
    Optional<Likes> findByUserIdAndGroupNotice(Long userId, GroupNotice groupNotice);
}
