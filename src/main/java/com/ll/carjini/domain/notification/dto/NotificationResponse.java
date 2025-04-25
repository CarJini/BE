package com.ll.carjini.domain.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean isRead;
    private Long maintenanceItemId;
    private LocalDateTime createdAt;
}
