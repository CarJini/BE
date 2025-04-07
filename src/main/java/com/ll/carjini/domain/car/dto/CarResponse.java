package com.ll.carjini.domain.car.dto;

import com.ll.carjini.domain.car.entity.Car;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CarResponse {
    private Long id;
    private String image;
    private String brand;
    private String model;

    public static CarResponse of(Car car) {
        return new CarResponse(
                car.getId(),
                car.getCarImage(),
                car.getBrand(),
                car.getModel()
        );
    }
}
