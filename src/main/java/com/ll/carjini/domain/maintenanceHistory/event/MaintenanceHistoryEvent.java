package com.ll.carjini.domain.maintenanceHistory.event;

import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaintenanceHistoryEvent {
    private final Long carOwnerId;
}
