package com.ebiz.wsb.domain.guardian.repository;

import com.ebiz.wsb.domain.guardian.entity.Guardian;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findGuardianByEmail(String email);

    List<Guardian> findByGroupId(Long groupId);
}
