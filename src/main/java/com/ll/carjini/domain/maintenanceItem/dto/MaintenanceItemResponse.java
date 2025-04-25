package com.ll.carjini.domain.maintenanceItem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceItemResponse {
    private Long id;                    // 항목 ID
    private String name;
    private String category;// 항목 이름
    private String replacementCycle;     // 교체 주기 정보
    private String lastReplacementDate;  // 최근 교체 날짜
    private String status;               // 상태 (교체 필요 / 주의 / 양호)
    private int progress;                // 진행 바 (0~100%)
}
