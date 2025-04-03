package com.ll.carjini.domain.maintenanceItem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaintenanceItemDetailResponse {
    private String name;                 // 항목 이름
    private String replacementCycle;     // 교체 주기 정보
    private Long remainingKm;            // 남은 거리 (0 이하일 경우 "교체 필요")
    private String lastReplacementDate;  // 최근 교체 날짜
    private String status;               // 상태 (교체 필요 / 주의 / 양호)
    private int kmProgress;
    private int dayProgress;
}
