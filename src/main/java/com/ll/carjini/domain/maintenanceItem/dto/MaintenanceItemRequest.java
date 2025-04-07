package com.ll.carjini.domain.maintenanceItem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceItemRequest {
    private String name;
    private String maintenanceItemCategory;
    private Long replacementCycle; // 교체 기간
    private Long replacementKm;
}
