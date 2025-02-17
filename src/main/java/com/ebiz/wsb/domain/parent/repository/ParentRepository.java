package com.ebiz.wsb.domain.parent.repository;

import com.ebiz.wsb.domain.parent.entity.Parent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findParentByEmail(String email);
    List<Parent> findByGroupId(Long groupId);
}
