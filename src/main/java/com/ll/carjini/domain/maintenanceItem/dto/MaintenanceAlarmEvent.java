package com.ll.carjini.domain.maintenanceItem.dto;

import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import lombok.Getter;

@Getter
public class MaintenanceAlarmEvent {
    private final MaintenanceItem maintenanceItem;
    private final String alarmType;  // "CYCLE" 또는 "KM"
    private final Long currentValue;
    private final Long thresholdValue;

    public MaintenanceAlarmEvent(MaintenanceItem maintenanceItem, String alarmType,
                                 Long currentValue, Long thresholdValue) {
        this.maintenanceItem = maintenanceItem;
        this.alarmType = alarmType;
        this.currentValue = currentValue;
        this.thresholdValue = thresholdValue;
    }
}

