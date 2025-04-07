package com.ll.carjini.domain.member.controller;

import com.ll.carjini.domain.member.dto.UpdateNicknameRequest;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.service.MemberService;
import com.ll.carjini.domain.oauth.entity.PrincipalDetails;
import com.ll.carjini.global.dto.GlobalResponse;
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

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public GlobalResponse<MemberDto> getUserProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member member = principalDetails.user();
        MemberDto userProfile = MemberDto.of(member);

        return GlobalResponse.success(userProfile);
    }

    @PutMapping("/nickname")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberDto> updateNickname(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody @Valid UpdateNicknameRequest request
    ) {
        Long memberId = principalDetails.user().getId();
        MemberDto updatedMember = memberService.updateNickname(memberId, request.getNickname());

        return ResponseEntity.ok(updatedMember);
    }


}
