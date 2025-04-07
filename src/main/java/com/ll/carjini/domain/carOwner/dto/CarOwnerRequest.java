package com.ll.carjini.domain.carOwner.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CarOwnerRequest {
    private Long carId;
    private LocalDate startDate;
    private Long startKm;
    private Long nowKm;
}
