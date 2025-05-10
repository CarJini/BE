package com.ll.carjini.domain.maintenanceItem.service;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.carOwner.repository.CarOwnerRepository;
import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import com.ll.carjini.domain.maintenanceHistory.repository.MaintenanceHistoryRepository;
import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceItemDetailResponse;
import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceItemResponse;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItemCategory;
import com.ll.carjini.domain.maintenanceItem.repository.MaintenanceItemRepository;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MaintenanceItemService {

    private final MaintenanceItemRepository maintenanceItemRepository;
    private final CarOwnerRepository carOwnerRepository;

    private CarOwner validateCarOwnerAccess(Long carOwnerId, Long memberId) {
        CarOwner carOwner = carOwnerRepository.findByIdWithMember(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        if (!carOwner.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return carOwner;
    }

    @Transactional
    public MaintenanceItem create(Long carOwnerId, Long memberId, String name, MaintenanceItemCategory category,
                                  Long replacementCycle, boolean cycleAlarm,  Long replacementKm, boolean kmAlarm) {

        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);

        MaintenanceItem maintenanceItem = MaintenanceItem.builder()
                .carOwner(carOwner)
                .name(name)
                .maintenanceItemCategory(category)
                .replacementCycle(replacementCycle)
                .cycleAlarm(cycleAlarm)
                .replacementKm(replacementKm)
                .kmAlarm(kmAlarm)
                .build();

        maintenanceItem = maintenanceItemRepository.save(maintenanceItem);

        return maintenanceItem;
    }

    @Transactional
    public MaintenanceItem update(Long carOwnerId, Long memberId, Long id, String name, MaintenanceItemCategory category,
                                  Long replacementCycle, boolean cycleAlarm,  Long replacementKm, boolean kmAlarm) {

        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);

        MaintenanceItem item = maintenanceItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 정비 항목을 찾을 수 없습니다. ID: " + id));

        item.setCarOwner(carOwner);
        item.setName(name);
        item.setMaintenanceItemCategory(category);
        item.setReplacementCycle(replacementCycle);
        item.setKmAlarm(kmAlarm);
        item.setCycleAlarm(cycleAlarm);
        item.setReplacementKm(replacementKm);

        return item;
    }

    @Transactional
    public boolean delete(Long carOwnerId, Long memberId, Long id) {
        validateCarOwnerAccess(carOwnerId, memberId);

        if (maintenanceItemRepository.existsById(id)) {
            maintenanceItemRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Page<MaintenanceItemDetailResponse> getMaintenanceItem(Long carOwnerId, Long memberId, Pageable pageable) {
        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);
        Long km = carOwner.getNowKm() + carOwner.getStartKm();
        log.info("차량 소유자 ID: {}, 현재 주행 거리: {}", carOwner.getId(), km);
        LocalDate date= carOwner.getStartDate();

        return maintenanceItemRepository.findByCarOwner(carOwner, pageable)
                .map(item -> convertToDetailedResponse(km, date, item));
    }


    private MaintenanceItemDetailResponse convertToDetailedResponse(Long km, LocalDate date, MaintenanceItem item) {
        MaintenanceHistory latestHistory = getLatestMaintenanceHistory(item);

        Long lastReplacementKm = (latestHistory != null && latestHistory.getReplacementKm() != null) ?
                latestHistory.getReplacementKm() : 0L;

        Long remainingKm = null;
        if (item.getReplacementKm() != null) {
            remainingKm = item.getReplacementKm() - (km - lastReplacementKm);
        }
        log.info("lastReplacementKm: {}, km:{}, remainingKm: {}", lastReplacementKm, km, remainingKm);

        LocalDate lastReplacementDateObj = date;
        if (latestHistory != null && latestHistory.getReplacementDate() != null) {
            lastReplacementDateObj = latestHistory.getReplacementDate();
        }

        Long remainingDays = calculateRemainingDays(item, lastReplacementDateObj);

        int progressKm = calculateKmProgress(item, remainingKm);

        int progressDays = calculateDayProgress(item, remainingDays);

        String status = determineStatus(progressKm, progressDays);

        return new MaintenanceItemDetailResponse(
                item.getId(),
                item.getName(),
                item.getMaintenanceItemCategory().name(),
                item.getReplacementKm(),
                item.getReplacementCycle(),
                remainingKm,
                remainingDays,
                item.isKmAlarm(),
                item.isCycleAlarm(),
                status,
                progressKm,
                progressDays
        );
    }

    private MaintenanceHistory getLatestMaintenanceHistory(MaintenanceItem item) {
        if (item.getMaintenanceHistories() == null || item.getMaintenanceHistories().isEmpty()) {
            return null;
        }

        return item.getMaintenanceHistories().stream()
                .filter(history -> history.getReplacementDate() != null)
                .max(Comparator.comparing(MaintenanceHistory::getReplacementDate))
                .orElse(null);
    }


    private Long calculateRemainingDays(MaintenanceItem item, LocalDate lastReplacementDate) {
        if (item.getReplacementCycle() == null || lastReplacementDate == null) {
            return Long.MAX_VALUE;
        }

        LocalDate nextReplacementDate = lastReplacementDate.plusMonths(item.getReplacementCycle());
        return ChronoUnit.DAYS.between(LocalDate.now(), nextReplacementDate);
    }

    private String determineStatus(int progressKm, int progressDays) {

        if (progressKm >= 100 || progressDays >= 100) {
            return "점검 필요";
        } else if (progressKm >= 80 || progressDays >= 80) {
            return "예상";
        } else {
            return "정상";
        }
    }

    private int calculateKmProgress(MaintenanceItem item, Long remainingKm) {
        Integer kmProgress = null;

        if (item.getReplacementKm() != null && remainingKm != null) {
            double kmRatio = (double)(item.getReplacementKm() - remainingKm) / item.getReplacementKm();
            kmProgress = (int) Math.max(0, kmRatio * 100);
        }

        return kmProgress;
    }

    private int calculateDayProgress(MaintenanceItem item, Long remainingDays) {
        Integer dayProgress = null;

        if (item.getReplacementCycle() != null && remainingDays != null && remainingDays != Long.MAX_VALUE) {
            long totalDays = item.getReplacementCycle() * 30L;
            double dayRatio = (double) (totalDays- remainingDays) / totalDays;
            dayProgress = (int) Math.max(0, dayRatio * 100);
        }

        return dayProgress;
    }


}