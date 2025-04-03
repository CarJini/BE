package com.ll.carjini.domain.carOwner.service;

import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.car.repository.CarRepository;
import com.ll.carjini.domain.carOwner.dto.CarOwnerRequest;
import com.ll.carjini.domain.carOwner.dto.CarOwnerResponse;
import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.carOwner.repository.CarOwnerRepository;
import com.ll.carjini.domain.member.dto.MemberDto;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarOwnerService {
    private final CarOwnerRepository carOwnerRepository;
    private final CarRepository carRepository;

    @Transactional
    public CarOwnerResponse createCarOwner(Member member, CarOwnerRequest carOwnerRequest) {
        Car car = carRepository.findById(carOwnerRequest.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        CarOwner carOwner = CarOwner.builder()
                .member(member)
                .car(car)
                .startDate(carOwnerRequest.getStartDate())
                .startKm(carOwnerRequest.getStartKm())
                .nowKm(carOwnerRequest.getNowKm())
                .build();

        CarOwner savedCarOwner = carOwnerRepository.save(carOwner);

        return CarOwnerResponse.of(savedCarOwner);
    }

    @Transactional
    public CarOwnerResponse updateCarOwner(Member member, CarOwnerRequest carOwnerRequest) {
        Car car = carRepository.findById(carOwnerRequest.getCarId())
                .orElseThrow(() -> new RuntimeException("Car not found"));

        CarOwner carOwner = CarOwner.builder()
                .member(member)
                .car(car)
                .startDate(carOwnerRequest.getStartDate())
                .startKm(carOwnerRequest.getStartKm())
                .nowKm(carOwnerRequest.getNowKm())
                .build();

        CarOwner savedCarOwner = carOwnerRepository.save(carOwner);

        return CarOwnerResponse.of(savedCarOwner);
    }


    public CarOwnerResponse getCarOwner(Long carOwnerId) {
        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new RuntimeException("CarOwner not found"));

        return CarOwnerResponse.of(carOwner);
    }


    public List<CarOwnerResponse> getCarOwnersByMember(Member member) {
        List<CarOwner> carOwners = carOwnerRepository.findByMember(member);

        return carOwners.stream()
                .map(CarOwnerResponse::of)
                .collect(Collectors.toList());
    }



    @Transactional
    public void deleteCarOwner(Long carOwnerId, Member member) {
        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new RuntimeException("CarOwner not found"));

        if (!carOwner.getMember().getId().equals(member.getId())) {
            throw new RuntimeException("You don't have permission to delete this car owner record");
        }

        carOwnerRepository.delete(carOwner);
    }
}

