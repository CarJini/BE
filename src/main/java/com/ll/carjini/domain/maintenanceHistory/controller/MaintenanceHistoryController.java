package com.ll.carjini.domain.maintenanceHistory.controller;

import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryRequest;
import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryResponse;
import com.ll.carjini.domain.maintenanceHistory.service.MaintenanceHistoryService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/car/{carOwnerId}/maintenance-history/")
@RequiredArgsConstructor
public class MaintenanceHistoryController {
    private final MaintenanceHistoryService maintenanceHistoryService;

    @GetMapping
    public GlobalResponse<List<MaintenanceHistoryResponse>> getMaintenanceHistories(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId
    ) {
        try {
            List<MaintenanceHistoryResponse> responses = maintenanceHistoryService.getMaintenanceHistories(principalDetails.user().getId(), carOwnerId);
            return GlobalResponse.success(responses);
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }

    @PostMapping
    public GlobalResponse<String> createHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody MaintenanceHistoryRequest dto,
            @PathVariable Long carOwnerId) {

        maintenanceHistoryService.createMaintenanceHistory(principalDetails.user().getId(), carOwnerId, dto);
        return GlobalResponse.success("정비 이력이 추가되었습니다.");
    }

    @PutMapping("/{id}")
    public GlobalResponse<String> updateHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id,
            @PathVariable Long carOwnerId,
            @RequestBody MaintenanceHistoryRequest dto
    ) {
        try {
            maintenanceHistoryService.updateMaintenanceHistory(principalDetails.user().getId(), carOwnerId, id, dto);
            return GlobalResponse.success("정비 이력이 수정되었습니다.");
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public GlobalResponse<String> deleteHistory(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @PathVariable Long id) {
        try {
            maintenanceHistoryService.deleteMaintenanceHistory(principalDetails.user().getId(), carOwnerId, id);
            return GlobalResponse.success("정비 이력이 삭제되었습니다.");
        } catch(Exception e) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }
}
