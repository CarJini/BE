package com.ll.carjini.domain.member.repository;

import com.ll.carjini.domain.member.entity.AuthProvider;
import com.ll.carjini.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);

}
