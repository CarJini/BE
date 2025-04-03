package com.ll.carjini.domain.carOwner.dto;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
public class CarOwnerResponse {
    private Long carOwnerId;
    private String carModel;
    private String carNumber;
    private String memberName;
    private LocalDate startDate;
    private Long startKm;
    private Long nowKm;

    public static CarOwnerResponse of(CarOwner carOwner) {
        return new CarOwnerResponse(
                carOwner.getId(),
                carOwner.getCar().getBrand(),
                carOwner.getCar().getModel(),
                carOwner.getMember().getName(),
                carOwner.getStartDate(),
                carOwner.getStartKm(),
                carOwner.getNowKm()
        );
    }
}

