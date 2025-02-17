package com.ebiz.wsb.domain.group.repository;

import com.ebiz.wsb.domain.group.entity.Group;
import com.ebiz.wsb.domain.guardian.entity.Guardian;
import com.ebiz.wsb.domain.parent.entity.Parent;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Group g JOIN g.guardians gu WHERE g.id = :groupId AND gu.id = :guardianId")
    boolean isUserInGroupForGuardian(@Param("guardianId") Long guardianId, @Param("groupId") Long groupId);

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN TRUE ELSE FALSE END " +
            "FROM Group g JOIN g.parents pa WHERE g.id = :groupId AND pa.id = :parentId")
    boolean isUserInGroupForParent(@Param("parentId") Long parentId, @Param("groupId") Long groupId);

    // 그룹의 출근하기를 누른 인솔자 id와 출근 여부를 초기화하는 쿼리
    @Modifying
    @Transactional
    @Query("UPDATE Group g SET g.isGuideActive = false, g.dutyGuardianId = null")
    void resetGuideStatusForAllGroups();
}
