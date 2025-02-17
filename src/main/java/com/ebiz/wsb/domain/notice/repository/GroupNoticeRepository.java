package com.ebiz.wsb.domain.notice.repository;

import com.ebiz.wsb.domain.notice.entity.GroupNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface GroupNoticeRepository extends JpaRepository<GroupNotice, Long> {

    Page<GroupNotice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<GroupNotice> findAllByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

}
