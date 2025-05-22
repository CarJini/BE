package com.ll.carjini.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.repository.MemberRepository;
import com.ll.carjini.domain.notification.dto.NotificationResponse;
import com.ll.carjini.domain.notification.entity.Notification;
import com.ll.carjini.domain.notification.repository.NotificationRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendMaintenanceNotification(Member member, String message, MaintenanceItem item) {
        if (StringUtils.isEmpty(member.getFcmToken())) {
            log.warn("FCM 토큰이 없어 알림은 전송되지 않지만, 기록은 저장합니다.");
        } else {
            try {
                com.google.firebase.messaging.Notification firebaseNotification =
                        com.google.firebase.messaging.Notification.builder()
                                .setTitle("차량 유지보수 알림")
                                .setBody(message)
                                .build();

                Message fcmMessage = Message.builder()
                        .setNotification(firebaseNotification)
                        .putData("type", "MAINTENANCE_ALARM")
                        .putData("time", String.valueOf(System.currentTimeMillis()))
                        .setToken(member.getFcmToken())
                        .build();

                String response = firebaseMessaging.send(fcmMessage);
                log.info("Firebase 알림 전송 완료: {}, 응답: {}", message, response);
            } catch (FirebaseMessagingException e) {
                log.error("Firebase 알림 전송 실패: {}", message, e);
            }
        }

        // 알림 저장은 항상 수행
        saveMaintenanceNotification(member, "차량 유지보수 알림", message, Notification.NotificationType.MAINTENANCE_ALARM, item);
    }

    @Override
    public void sendSystemNotification(Member member, String message) {
        if (StringUtils.isEmpty(member.getFcmToken())) {
            log.warn("FCM 토큰이 없어 알림은 전송되지 않지만, 기록은 저장합니다.");
        } else {
            try {
                com.google.firebase.messaging.Notification firebaseNotification =
                        com.google.firebase.messaging.Notification.builder()
                                .setTitle("차량 유지보수 알림")
                                .setBody(message)
                                .build();

                Message fcmMessage = Message.builder()
                        .setNotification(firebaseNotification)
                        .putData("type", "MAINTENANCE_ALARM")
                        .putData("time", String.valueOf(System.currentTimeMillis()))
                        .setToken(member.getFcmToken())
                        .build();

                String response = firebaseMessaging.send(fcmMessage);
                log.info("Firebase 알림 전송 완료: {}, 응답: {}", message, response);
            } catch (FirebaseMessagingException e) {
                log.error("Firebase 알림 전송 실패: {}", message, e);
            }
        }

        // 알림 저장은 항상 수행
        saveSystemNotification(member, "차량 유지보수 알림", message, Notification.NotificationType.SYSTEM);
    }


    private Notification saveMaintenanceNotification(Member member, String title, String message,
                                          Notification.NotificationType type, MaintenanceItem maintenanceItem) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setMaintenanceItem(maintenanceItem);

        return notificationRepository.save(notification);
    }

    private Notification saveSystemNotification(Member member, String title, String message,
                                                     Notification.NotificationType type) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Override
    public List<NotificationResponse> getNotificationsByMemberId(Long memberId) {
        List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotificationsByMemberId(Long memberId) {
        List<Notification> notifications = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse getNotification(Long notificationId, Long memberId) {
        Notification notification = findNotificationByIdAndValidateMember(notificationId, memberId);
        return convertToDto(notification);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = findNotificationByIdAndValidateMember(notificationId, memberId);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long memberId) {
        List<Notification> unreadNotifications = notificationRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long memberId) {
        Notification notification = findNotificationByIdAndValidateMember(notificationId, memberId);
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long memberId) {
        notificationRepository.deleteByMemberId(memberId);
    }

    private NotificationResponse convertToDto(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .isRead(notification.isRead())
                .maintenanceItemId(notification.getMaintenanceItem() != null ? notification.getMaintenanceItem().getId() : null)
                .createdAt(notification.getCreatedAt())
                .build();
    }

    private Notification findNotificationByIdAndValidateMember(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다: " + notificationId));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 알림에 접근 권한이 없습니다");
        }

        return notification;
    }
}