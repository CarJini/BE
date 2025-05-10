package com.ll.carjini.domain.carOwner.service;

import com.ll.carjini.domain.car.entity.Car;
import com.ll.carjini.domain.car.repository.CarRepository;
import com.ll.carjini.domain.carOwner.dto.CarOwnerRequest;
import com.ll.carjini.domain.carOwner.dto.CarOwnerResponse;
import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.carOwner.repository.CarOwnerRepository;
import com.ll.carjini.domain.chatbot.repository.ChatRepository;
import com.ll.carjini.domain.maintenanceHistory.repository.MaintenanceHistoryRepository;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.maintenanceItem.repository.MaintenanceItemRepository;
import com.ll.carjini.domain.member.entity.Member;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
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
    private final MaintenanceItemRepository maintenanceItemRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public CarOwnerResponse createCarOwner(Member member, CarOwnerRequest carOwnerRequest) {
        Car car = carRepository.findById(carOwnerRequest.getCarId())
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

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
    public CarOwnerResponse updateCarOwner(Member member, Long carOwnerId, CarOwnerRequest carOwnerRequest) {

        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        Car car = carRepository.findById(carOwnerRequest.getCarId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        carOwner.setCar(car);
        carOwner.setStartDate(carOwnerRequest.getStartDate());
        carOwner.setStartKm(carOwnerRequest.getStartKm());
        carOwner.setNowKm(carOwnerRequest.getNowKm());

        CarOwner savedCarOwner = carOwnerRepository.save(carOwner);

        return CarOwnerResponse.of(savedCarOwner);
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
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!carOwner.getMember().getId().equals(member.getId())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        List<MaintenanceItem> maintenanceItems = maintenanceItemRepository.findByCarOwner(carOwner);
        for (MaintenanceItem item : maintenanceItems) {
            maintenanceHistoryRepository.deleteByMaintenanceItem(item);
        }
        maintenanceItemRepository.deleteAll(maintenanceItems);
//        chatRepository.deleteByCarOwnerId(carOwnerId);

        carOwnerRepository.delete(carOwner);
    }
}

