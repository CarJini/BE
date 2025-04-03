package com.ll.carjini.domain.carOwner.repository;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarOwnerRepository extends JpaRepository<CarOwner, Long> {
    List<CarOwner> findByMember(Member member);

    @Query("SELECT c FROM CarOwner c JOIN FETCH c.member WHERE c.id = :carOwnerId")
    Optional<CarOwner> findByIdWithMember(@Param("carOwnerId") Long carOwnerId);
}
