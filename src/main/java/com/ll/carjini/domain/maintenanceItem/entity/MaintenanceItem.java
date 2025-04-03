package com.ll.carjini.domain.maintenanceItem.entity;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class MaintenanceItem extends BaseEntity {

    private String name;

    @ManyToOne
    @JoinColumn(name = "car_owner_id", nullable = false)
    private CarOwner carOwner;

    @Enumerated(EnumType.STRING)
    private MaintenanceItemCategory maintenanceItemCategory;

    @Enumerated(EnumType.STRING)
    private IconColor iconColor;

    @Column(nullable = true)
    private Long replacementCycle;

    @Column(nullable = true)
    private Long replacementKm;

    @OneToMany(mappedBy = "maintenanceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaintenanceHistory> maintenanceHistories;
}