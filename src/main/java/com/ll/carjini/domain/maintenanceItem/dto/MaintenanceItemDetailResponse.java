package com.ll.carjini.domain.maintenanceItem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceItemDetailResponse {
    private Long id;
    private String name;
    private String category;// 항목 이름
    private Long replacementKm;     // 교체 주기 정보
    private Long replacementCycle;
    private Long remainingKm;            // 남은 거리 (0 이하일 경우 "교체 필요")
    private Long remainingDay;
    private boolean alarm;
    private String status;               // 상태 (교체 필요 / 주의 / 양호)
    private int kmProgress;
    private int dayProgress;

}
