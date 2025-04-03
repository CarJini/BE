package com.ll.carjini.domain.carOwner.entity;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.global.base.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Getter
@Setter
public class CarOwner extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id")
    private Car car;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalDate startDate;
    private Long startKm;
    private Long nowKm;
}