package com.ll.carjini.domain.car.service;

import com.ll.carjini.domain.car.dto.CarResponse;
import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.car.repository.CarRepository;
import com.ll.carjini.domain.carOwner.dto.CarOwnerResponse;
import com.ll.carjini.domain.carOwner.entity.CarOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarService {

    private final CarRepository carRepository;

    public List<CarResponse> getAllCars() {
        List<Car> cars = carRepository.findAll();

        if (cars.isEmpty()) {
            throw new RuntimeException("등록된 차량이 없습니다.");
        }

        return cars.stream()
                .map(CarResponse::of)
                .collect(Collectors.toList());
    }
}
