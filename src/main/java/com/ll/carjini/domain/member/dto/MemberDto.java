package com.ll.carjini.domain.member.dto;

import com.ll.carjini.domain.member.entity.Member;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private String name;
    private String email;
    private String profile;

    public static MemberDto of(Member member) {
        return MemberDto.builder()
                .name(member.getName())
                .email(member.getEmail())
                .profile(member.getProfile())
                .build();
    }
}