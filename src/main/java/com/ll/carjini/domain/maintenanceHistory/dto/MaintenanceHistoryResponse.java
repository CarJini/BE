package com.ll.carjini.domain.maintenanceHistory.dto;

import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceHistoryResponse {
    private Long id;
    private Long maintenanceItemId;
    private String maintenanceItemName;
    private LocalDate replacementDate;
    private Long replacementKm;
}
