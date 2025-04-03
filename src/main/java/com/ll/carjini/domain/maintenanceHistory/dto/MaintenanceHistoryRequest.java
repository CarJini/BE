package com.ll.carjini.domain.maintenanceHistory.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceHistoryRequest {
    private String maintenanceItemName;
    private Long maintenanceItemId;
    private LocalDate replacementDate;
    private Long replacementKm;
}
