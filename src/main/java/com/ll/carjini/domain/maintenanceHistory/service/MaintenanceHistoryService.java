package com.ll.carjini.domain.maintenanceHistory.service;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.carOwner.repository.CarOwnerRepository;
import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryRequest;
import com.ll.carjini.domain.maintenanceHistory.dto.MaintenanceHistoryResponse;
import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import com.ll.carjini.domain.maintenanceHistory.repository.MaintenanceHistoryRepository;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.maintenanceItem.repository.MaintenanceItemRepository;
import com.ll.carjini.domain.maintenanceItem.service.MaintenanceItemService;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MaintenanceHistoryService {
    private final MaintenanceItemRepository maintenanceItemRepository;
    private final MaintenanceItemService maintenanceItemService;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final CarOwnerRepository carOwnerRepository;

    public List<MaintenanceHistoryResponse> getMaintenanceHistories(Long userId, Long carOwnerId, Long maintenanceItemId) {
        MaintenanceItem maintenanceItem = maintenanceItemRepository.findById(maintenanceItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!carOwner.getMember().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        List<MaintenanceHistory> histories = maintenanceItem.getMaintenanceHistories();
        return histories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MaintenanceHistoryResponse convertToResponse(MaintenanceHistory history) {
        return MaintenanceHistoryResponse.builder()
                .id(history.getId())
                .maintenanceItemId(history.getMaintenanceItem().getId())
                .maintenanceItemName(history.getMaintenanceItem().getName())
                .replacementDate(history.getReplacementDate())
                .replacementKm(history.getReplacementKm())
                .build();
    }

    @Transactional
    public void createMaintenanceHistory(Long userId, Long carOwnerId, Long maintenanceItemId, MaintenanceHistoryRequest dto) {
        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        // 사용자가 이 차량에 접근 권한이 있는지 확인
        if (!carOwner.getMember().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // 정비 항목 찾기
        MaintenanceItem maintenanceItem = maintenanceItemRepository.findById(maintenanceItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        // 정비 이력 생성
        MaintenanceHistory history = MaintenanceHistory.builder()
                .maintenanceItem(maintenanceItem)
                .carOwner(carOwner)
                .replacementDate(dto.getReplacementDate())
                .replacementKm(dto.getReplacementKm())
                .build();

        maintenanceHistoryRepository.save(history);
        maintenanceHistoryRepository.flush();
        maintenanceItemService.updateProgress(carOwner.getNowKm()+ carOwner.getStartKm(),
                carOwner.getStartDate(),
                dto.getReplacementDate(),
                maintenanceItem);
    }

    @Transactional
    public void updateMaintenanceHistory(Long userId, Long carOwnerId, Long maintenanceItemId, Long historyId, MaintenanceHistoryRequest dto) {
        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!carOwner.getMember().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        MaintenanceHistory history = maintenanceHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!history.getCarOwner().getId().equals(carOwnerId)) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }

        MaintenanceItem item = maintenanceItemRepository.findById(maintenanceItemId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
        history.setReplacementDate(dto.getReplacementDate());
        history.setReplacementKm(dto.getReplacementKm());

        maintenanceHistoryRepository.save(history);
        maintenanceHistoryRepository.flush();
        maintenanceItemService.updateProgress(carOwner.getNowKm() + carOwner.getStartKm(),
                carOwner.getStartDate(),
                dto.getReplacementDate(),
                item);
    }

    @Transactional
    public void deleteMaintenanceHistory(Long userId, Long carOwnerId, Long maintenanceItemId, Long historyId) {
        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!carOwner.getMember().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        MaintenanceItem maintenanceItem = maintenanceItemRepository.findById(maintenanceItemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        MaintenanceHistory history = maintenanceHistoryRepository.findById(historyId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!history.getCarOwner().getId().equals(carOwnerId)) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND);
        }

        maintenanceHistoryRepository.delete(history);

        LocalDate replacementDate = maintenanceHistoryRepository
                .findTopByMaintenanceItemIdOrderByReplacementDateDescReplacementKmDesc(maintenanceItemId)
                .map(MaintenanceHistory::getReplacementDate)
                .orElse(carOwner.getStartDate());
        maintenanceHistoryRepository.flush();
        maintenanceItemService.updateProgress(
                carOwner.getNowKm(),
                carOwner.getStartDate(),
                replacementDate,
                maintenanceItem
        );
    }
}