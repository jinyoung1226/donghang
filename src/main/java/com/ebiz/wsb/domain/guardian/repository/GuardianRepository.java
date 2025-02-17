package com.ebiz.wsb.domain.guardian.repository;

import com.ebiz.wsb.domain.guardian.entity.Guardian;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findGuardianByEmail(String email);

    List<Guardian> findByGroupId(Long groupId);

    @Query("SELECT g FROM Guardian g WHERE g.group.id = :groupId")
    List<Guardian> findGuardiansByGroupId(@Param("groupId") Long groupId);
}
