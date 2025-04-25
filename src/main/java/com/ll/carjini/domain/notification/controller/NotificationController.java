package com.ll.carjini.domain.notification.controller;

import com.ll.carjini.domain.notification.dto.NotificationResponse;
import com.ll.carjini.domain.notification.service.NotificationService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 사용자의 모든 알림 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        List<NotificationResponse> notifications = notificationService.getNotificationsByMemberId(memberId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 목록 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        List<NotificationResponse> notifications = notificationService.getUnreadNotificationsByMemberId(memberId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 단일 알림 조회
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        NotificationResponse notification = notificationService.getNotification(notificationId, memberId);
        return ResponseEntity.ok(notification);
    }

    /**
     * 알림 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        notificationService.markAsRead(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        notificationService.deleteNotification(notificationId, memberId);
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 삭제
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        Long memberId = principalDetails.user().getId();
        notificationService.deleteAllNotifications(memberId);
        return ResponseEntity.ok().build();
    }

}
