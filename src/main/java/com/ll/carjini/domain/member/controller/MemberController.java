package com.ll.demo03.domain.member.controller;

import com.ll.demo03.domain.member.dto.UpdateNicknameRequest;
import com.ll.demo03.domain.member.entity.Member;
import com.ll.demo03.domain.member.service.MemberService;
import com.ll.demo03.domain.oauth.entity.PrincipalDetails;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import com.ll.demo03.domain.member.dto.MemberDto;
import com.ll.demo03.global.dto.GlobalResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
@Tag(name = " 회원 API", description = "User")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemberDto> getUserProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        Member member = principalDetails.user();
        MemberDto userProfile = MemberDto.of(member);

        return ResponseEntity.ok(userProfile);
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
