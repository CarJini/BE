package com.ll.carjini.domain.maintenanceItem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaintenanceItemRequest {
    private String name;
    private String maintenanceItemCategory;
    private String iconColor;
    private Long replacementCycle; // 교체 기간
    private Long replacementKm;
}
