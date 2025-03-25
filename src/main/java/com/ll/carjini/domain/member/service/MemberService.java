package com.ll.carjini.domain.member.service;

import com.ll.carjini.domain.member.dto.MemberDto;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional
    public MemberDto updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));

        member.updateNickname(nickname);
        Member savedMember = memberRepository.save(member);

        return MemberDto.of(savedMember);
    }
}
