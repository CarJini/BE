package com.ll.carjini.domain.notification.entity;

import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.global.base.BaseEntity;
import com.ll.carjini.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_item_id")
    private MaintenanceItem maintenanceItem;

    public enum NotificationType {
        MAINTENANCE_ALARM, SYSTEM
    }
}