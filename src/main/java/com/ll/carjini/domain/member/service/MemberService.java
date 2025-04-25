package com.ll.carjini.domain.member.service;

import com.ll.carjini.domain.member.dto.MemberDto;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import jakarta.persistence.EntityNotFoundException;
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
    public void updateFcmToken(Long memberId, String fcmToken) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다: " + memberId));

        member.setFcmToken(fcmToken);
        memberRepository.save(member);
    }

    @Transactional
    public MemberDto updateNickname(Long memberId, String nickname) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACCESS_DENIED));

        member.updateNickname(nickname);
        Member savedMember = memberRepository.save(member);

        return MemberDto.of(savedMember);
    }
}
