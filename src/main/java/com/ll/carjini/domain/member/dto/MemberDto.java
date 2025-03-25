package com.ll.carjini.domain.member.dto;

import com.ll.carjini.domain.member.entity.Member;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private String nickname;

    public static MemberDto of(Member member) {
        return MemberDto.builder()
                .nickname(member.getNickname())
                .build();
    }
}