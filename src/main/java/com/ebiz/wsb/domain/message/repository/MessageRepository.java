package com.ebiz.wsb.domain.message.repository;

import com.ebiz.wsb.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByStudent_StudentIdAndGuardian_Id(Long studentId, Long guardianId);

}
