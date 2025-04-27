package com.ll.carjini.domain.maintenanceItem.controller;

import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceItemDetailResponse;
import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceItemRequest;
import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceItemResponse;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItemCategory;
import com.ll.carjini.domain.maintenanceItem.service.MaintenanceItemService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/car/{carOwnerId}/maintenance-items")
@RequiredArgsConstructor
@Tag(name = "차량 정비 아이템 API", description = "차량 정비 아이템 조회/등록/수정/삭제")
public class MaintenanceItemController {

    private final MaintenanceItemService maintenanceItemService;

    @GetMapping
    @Operation(summary = "차량 정비 아이템들 조회", description = "사용자의 차량 정비 아이템들을 조회합니다. 스웨거 쓸 때 pageable 파라미터에 sort 에는 'createdAt' 넣거나, 아예 sort 항목을 지워도 됩니다.  ")
    public GlobalResponse<Page<MaintenanceItemDetailResponse>> getMaintenanceItems(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        try {
            Long memberId = principalDetails.user().getId();
            return GlobalResponse.success(maintenanceItemService.getMaintenanceItem(carOwnerId, memberId, pageable));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @PostMapping
    @Operation(summary = "차량 정비 아이템 등록", description = "사용자의 차량 정비 아이템을 등록합니다. 카테고리는 ENGINE_OIL, BRAKE_PAD, TIRE, AIR_FILTER, COOLANT로 임시로 설정해 놓았습니다. ")
    public GlobalResponse<String> create(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody MaintenanceItemRequest dto,
            @PathVariable Long carOwnerId) {
        try {
            Long memberId = principalDetails.user().getId();

            MaintenanceItemCategory category = MaintenanceItemCategory.valueOf(dto.getCategory().toUpperCase());

            maintenanceItemService.create(carOwnerId, memberId, dto.getName(), category, dto.getReplacementCycle(), dto.getReplacementKm());

            return GlobalResponse.success("정비 항목이 생성되었습니다.");
        }catch(Exception e){
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }


    @PutMapping("/{id}")
    @Operation(summary = "차량 정비 아이템 수정", description = "사용자의 차량 정비 아이템을 수정합니다.")
    public GlobalResponse<String> update(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long id,
            @PathVariable Long carOwnerId,
            @RequestBody MaintenanceItemRequest dto
    ) {
        try{
            Long memberId = principalDetails.user().getId();

            MaintenanceItemCategory category = MaintenanceItemCategory.valueOf(dto.getCategory().toUpperCase());

           maintenanceItemService.update(
                   carOwnerId,
                   memberId,
                   id,
                    dto.getName(),
                    category,
                    dto.getReplacementCycle(),
                    dto.getReplacementKm()
            );
            return GlobalResponse.success("정비 항목이 수정되었습니다.");
        }catch(Exception e){
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "차량 정비 아이템 삭제", description = "사용자의 차량 정비 아이템을 삭제합니다.")
    public GlobalResponse<String> delete(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @PathVariable Long id) {
        Long memberId = principalDetails.user().getId();

        boolean isDeleted = maintenanceItemService.delete(carOwnerId, memberId, id);

        if (isDeleted) {
            return GlobalResponse.success("정비 항목이 삭제되었습니다.");
        }
        throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
    }

}
