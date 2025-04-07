package com.ll.carjini.domain.carOwner.dto;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CarOwnerResponse {
    private Long carOwnerId;
    private String carModel;
    private String carNumber;
    private String carImage;
    private LocalDate startDate;
    private Long startKm;
    private Long nowKm;

    public static CarOwnerResponse of(CarOwner carOwner) {
        return new CarOwnerResponse(
                carOwner.getId(),
                carOwner.getCar().getBrand(),
                carOwner.getCar().getModel(),
                carOwner.getCar().getCarImage(),
                carOwner.getStartDate(),
                carOwner.getStartKm(),
                carOwner.getNowKm()
        );
    }
}

