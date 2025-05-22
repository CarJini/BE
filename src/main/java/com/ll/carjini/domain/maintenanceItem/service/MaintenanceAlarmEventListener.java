package com.ll.carjini.domain.maintenanceItem.service;

import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceAlarmEvent;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceAlarmEventListener {

     private final NotificationService notificationService;

    @EventListener
    public void handleMaintenanceAlarmEvent(MaintenanceAlarmEvent event) {
        MaintenanceItem item = event.getMaintenanceItem();
        String alarmType = event.getAlarmType();
        Long currentValue = event.getCurrentValue();
        Long thresholdValue = event.getThresholdValue();

        log.info("유지보수 알림 발생: {} - {} (현재값: {}, 기준값: {})",
                item.getName(), alarmType, currentValue, thresholdValue);

        String message = createAlarmMessage(item, alarmType, currentValue, thresholdValue);

         notificationService.sendMaintenanceNotification(item.getCarOwner().getMember(), message, item);

    }

    private String createAlarmMessage(MaintenanceItem item, String alarmType,
                                      Long currentValue, Long thresholdValue) {
        if ("CYCLE".equals(alarmType)) {
            return String.format("%s의 교체 주기(%d일)가 도래했습니다. 마지막 교체 후 %d일이 경과했습니다.",
                    item.getName(), thresholdValue, currentValue);
        } else {
            return String.format("%s의 교체 주행거리(%dkm)에 도달했습니다. 마지막 교체 후 %dkm을 주행했습니다.",
                    item.getName(), thresholdValue, currentValue);
        }
    }
}