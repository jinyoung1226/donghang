package com.ebiz.wsb.domain.alert.repository;

import com.ebiz.wsb.domain.alert.entity.Alert;
import com.ebiz.wsb.domain.notification.entity.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByReceiverIdAndUserType(Long receiverId, UserType userType);

    @Modifying
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.id IN :ids")
    void markAlertsAsRead(@Param("ids") List<Long> ids);

}
