package com.ll.carjini.domain.maintenanceHistory.controller;

import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryRequest;
import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryResponse;
import com.ll.carjini.domain.maintenanceHistory.service.MaintenanceHistoryService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/car/{carOwnerId}/maintenance-items/{maintenanceItemId}/maintenance-history")
@RequiredArgsConstructor
@Tag(name = "차량 정비 이력 API", description = "차량 정비 이력 조회/등록/수정/삭제")
public class MaintenanceHistoryController {
    private final MaintenanceHistoryService maintenanceHistoryService;

    @GetMapping
    @Operation(summary = "차량 정비 이력들 조회", description = "사용자의 차량 정비 이력들을 조회합니다.")
    public GlobalResponse<List<MaintenanceHistoryResponse>> getMaintenanceHistories(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @PathVariable Long maintenanceItemId
    ) {
        try {
            List<MaintenanceHistoryResponse> responses = maintenanceHistoryService.getMaintenanceHistories(principalDetails.user().getId(),carOwnerId, maintenanceItemId);
            return GlobalResponse.success(responses);
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    @PostMapping
    @Operation(summary = "차량 정비 이력 등록", description = "사용자의 차량 정비 이력을 등록합니다. 여기서 replacementKm, replacementDate는 교체 당시 차량의 주행거리와 날짜를 의미합니다. ")
    public GlobalResponse<String> createHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody MaintenanceHistoryRequest dto,
            @PathVariable Long carOwnerId,
            @PathVariable Long maintenanceItemId) {

        maintenanceHistoryService.createMaintenanceHistory(principalDetails.user().getId(), carOwnerId, maintenanceItemId, dto);
        return GlobalResponse.success("정비 이력이 추가되었습니다.");
    }

    @PutMapping("/{id}")
    @Operation(summary = "차량 정비 이력 수정", description = "사용자의 차량 정비 이력을 수정합니다.")
    public GlobalResponse<String> updateHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id,
            @PathVariable Long carOwnerId,
            @PathVariable Long maintenanceItemId,
            @RequestBody MaintenanceHistoryRequest dto
    ) {
        try {
            maintenanceHistoryService.updateMaintenanceHistory(principalDetails.user().getId(), carOwnerId,maintenanceItemId, id, dto);
            return GlobalResponse.success("정비 이력이 수정되었습니다.");
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "차량 정비 이력 삭제", description = "사용자의 차량 정비 이력을 삭제합니다.")
    public GlobalResponse<String> deleteHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @PathVariable Long maintenanceItemId,
            @PathVariable Long id) {
        try {
            maintenanceHistoryService.deleteMaintenanceHistory(principalDetails.user().getId(), carOwnerId, maintenanceItemId, id);
            return GlobalResponse.success("정비 이력이 삭제되었습니다.");
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }
}
