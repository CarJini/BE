package com.ll.carjini.domain.carOwner.controller;
import com.ll.carjini.domain.carOwner.dto.CarOwnerRequest;
import com.ll.carjini.domain.carOwner.dto.CarOwnerResponse;
import com.ll.carjini.domain.carOwner.service.CarOwnerService;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
import com.ll.carjini.global.error.ErrorCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/car-owner")
@Tag(name = "차등록 API", description = "내 차량 조회/등록/수정/삭제")
public class CarOwnerController {
    private final CarOwnerService carOwnerService;

    @PostMapping
    @Operation(summary = "차량 등록", description = "사용자의 차량을 등록합니다.")
    public GlobalResponse<?> createCarOwner(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody CarOwnerRequest carOwnerRequest
    ) {
        try {
            Member member = principalDetails.user();
            CarOwnerResponse response = carOwnerService.createCarOwner(member, carOwnerRequest);
            return GlobalResponse.success(response);
        } catch (Exception e) {
            return GlobalResponse.error(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @GetMapping
    @Operation(summary = "내 차량 목록 조회", description = "로그인한 사용자의 모든 차량 소유 정보를 조회합니다.")
    public GlobalResponse<?> getMyCarOwners(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        try {
            Member member = principalDetails.user();
            List<CarOwnerResponse> responses = carOwnerService.getCarOwnersByMember(member);
            return GlobalResponse.success(responses);
        } catch (Exception e) {
            return GlobalResponse.error(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @PutMapping("/{carOwnerId}")
    @Operation(summary = "차량 정보 수정", description = "사용자의 차량 정보를 수정합니다.")
    public GlobalResponse<?> updateCarOwner(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId,
            @RequestBody CarOwnerRequest carOwnerRequest
    ) {
        try {
            Member member = principalDetails.user();
            CarOwnerResponse response = carOwnerService.updateCarOwner(member, carOwnerId, carOwnerRequest);
            return GlobalResponse.success(response);
        } catch (Exception e) {
            return GlobalResponse.error(ErrorCode.ENTITY_NOT_FOUND);
        }
    }

    @DeleteMapping("/{carOwnerId}")
    @Operation(summary = "차량 정보 삭제", description = "사용자의 차량 정보를 삭제합니다.")
    public GlobalResponse<?> deleteCarOwner(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @PathVariable Long carOwnerId
    ) {
        try {
            Member member = principalDetails.user();
            carOwnerService.deleteCarOwner(carOwnerId, member);
            return GlobalResponse.success();
        } catch (Exception e) {
            return GlobalResponse.error(ErrorCode.ENTITY_NOT_FOUND);
        }
    }
}