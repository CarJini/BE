package com.ll.carjini.domain.notification.service;


import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.notification.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {
    void sendNotification(Member member, String message);

    // 새로운 메서드들
    List<NotificationResponse> getNotificationsByMemberId(Long memberId);

    List<NotificationResponse> getUnreadNotificationsByMemberId(Long memberId);

    NotificationResponse getNotification(Long notificationId, Long memberId);

    void markAsRead(Long notificationId, Long memberId);

    void markAllAsRead(Long memberId);

    void deleteNotification(Long notificationId, Long memberId);

    void deleteAllNotifications(Long memberId);
}
