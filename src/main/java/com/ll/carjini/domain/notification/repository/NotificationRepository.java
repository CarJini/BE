package com.ll.carjini.domain.notification.repository;

import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    void deleteByMemberId(Long memberId);

    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Notification> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(Long memberId);
    void deleteByMaintenanceItem_Id(Long maintenanceItemId);
}
