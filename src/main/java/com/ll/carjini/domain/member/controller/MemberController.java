package com.ll.carjini.domain.member.controller;

import com.ll.carjini.domain.member.dto.FCMTokenRequest;
import com.ll.carjini.domain.member.dto.UpdateNicknameRequest;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.service.MemberService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.member.dto.MemberDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@Tag(name = " 회원 API", description = "User")
public class MemberController {

    private final MemberService memberService;

    @PutMapping("/fcm-token")
    @Operation(summary = "FCM 토큰 업데이트", description = "FCM 토큰을 업데이트합니다.")
    public GlobalResponse<Void> updateFcmToken(
            @RequestBody FCMTokenRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        memberService.updateFcmToken(principalDetails.user().getId(), request.getFcmToken());
        return GlobalResponse.success();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "회원 정보 조회", description = "회원 정보를 조회합니다.")
    public GlobalResponse<MemberDto> getUserProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member member = principalDetails.user();
        MemberDto userProfile = MemberDto.of(member);

        return GlobalResponse.success(userProfile);
    }

    @PutMapping("/nickname")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "회원 닉네임 수정", description = "회원의 닉네임을 수정합니다.")
    public GlobalResponse<MemberDto> updateNickname(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody @Valid UpdateNicknameRequest request
    ) {
        Long memberId = principalDetails.user().getId();
        MemberDto updatedMember = memberService.updateNickname(memberId, request.getNickname());

        return GlobalResponse.success(updatedMember);
    }
}
