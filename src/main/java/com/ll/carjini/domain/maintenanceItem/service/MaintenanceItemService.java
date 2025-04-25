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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
public class MaintenanceItemService {

    private final MaintenanceItemRepository maintenanceItemRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final CarOwnerRepository carOwnerRepository;

    private CarOwner validateCarOwnerAccess(Long carOwnerId, Long memberId) {
        CarOwner carOwner = carOwnerRepository.findByIdWithMember(carOwnerId)
                .orElseThrow(() -> new EntityNotFoundException("해당 차량 소유 정보를 찾을 수 없습니다."));

        if (!carOwner.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("해당 차량의 정비 항목을 조회할 권한이 없습니다.");
        }

        return carOwner;
    }


    @Transactional
    public MaintenanceItem create(Long carOwnerId, Long memberId, String name, MaintenanceItemCategory category,
                                  Long replacementCycle, Long replacementKm) {

        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);

        MaintenanceItem maintenanceItem = MaintenanceItem.builder()
                .carOwner(carOwner)
                .name(name)
                .maintenanceItemCategory(category)
                .replacementCycle(replacementCycle)
                .replacementKm(replacementKm)
                .build();

        maintenanceItem = maintenanceItemRepository.save(maintenanceItem);

        MaintenanceHistory maintenanceHistory = MaintenanceHistory.builder()
                .maintenanceItem(maintenanceItem)
                .carOwner(carOwner)
                .build();

        maintenanceHistoryRepository.save(maintenanceHistory);

        return maintenanceItem;
    }

    @Transactional
    public MaintenanceItem update(Long carOwnerId, Long memberId, Long id, String name, MaintenanceItemCategory category,
                                   Long replacementCycle, Long replacementKm) {

        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);

        MaintenanceItem item = maintenanceItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 정비 항목을 찾을 수 없습니다. ID: " + id));

        item.setCarOwner(carOwner);
        item.setName(name);
        item.setMaintenanceItemCategory(category);
        item.setReplacementCycle(replacementCycle);
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

    public MaintenanceItemDetailResponse getMaintenanceItem(Long carOwnerId, Long memberId, Long id) {
        validateCarOwnerAccess(carOwnerId, memberId);

        MaintenanceItem item = maintenanceItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 정비 항목을 찾을 수 없습니다. ID: " + id));

        return convertToDetailedResponse(item);
    }


    public List<MaintenanceItemResponse> getMaintenanceItems(Long carOwnerId, Long memberId) {
        CarOwner carOwner = validateCarOwnerAccess(carOwnerId, memberId);

        return maintenanceItemRepository.findByCarOwner(carOwner).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MaintenanceItemResponse convertToResponse(MaintenanceItem item) {
        MaintenanceHistory latestHistory = getLatestMaintenanceHistory(item);

        Long lastReplacementKm = (latestHistory != null && latestHistory.getReplacementKm() != null) ?
                latestHistory.getReplacementKm() : 0L;

        Long remainingKm = null;
        if (item.getReplacementKm() != null) {
            remainingKm = item.getReplacementKm() - (lastReplacementKm != null ? lastReplacementKm : 0L);
        }

        String lastReplacementDate = "정보 없음";
        LocalDate lastReplacementDateObj = null;
        if (latestHistory != null && latestHistory.getReplacementDate() != null) {
            lastReplacementDateObj = latestHistory.getReplacementDate();
            lastReplacementDate = lastReplacementDateObj.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }

        String replacementCycleText = buildReplacementCycleText(item);

        Long remainingDays = calculateRemainingDays(item, lastReplacementDateObj);

        String status = determineStatus(remainingKm, remainingDays);

        int progress = calculateProgress(item, remainingKm, remainingDays);

        return new MaintenanceItemResponse(
                item.getName(),
                item.isCycleAlarm(),
                replacementCycleText,
                item.isKmAlarm(),
                remainingKm != null ? remainingKm : 0L,
                lastReplacementDate,
                status,
                progress
        );
    }

    private MaintenanceItemDetailResponse convertToDetailedResponse(MaintenanceItem item) {
        MaintenanceHistory latestHistory = getLatestMaintenanceHistory(item);

        Long lastReplacementKm = (latestHistory != null && latestHistory.getReplacementKm() != null) ?
                latestHistory.getReplacementKm() : 0L;

        Long remainingKm = null;
        if (item.getReplacementKm() != null) {
            remainingKm = item.getReplacementKm() - (lastReplacementKm != null ? lastReplacementKm : 0L);
        }

        String lastReplacementDate = "정보 없음";
        LocalDate lastReplacementDateObj = null;
        if (latestHistory != null && latestHistory.getReplacementDate() != null) {
            lastReplacementDateObj = latestHistory.getReplacementDate();
            lastReplacementDate = lastReplacementDateObj.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }

        String replacementCycleText = buildReplacementCycleText(item);

        Long remainingDays = calculateRemainingDays(item, lastReplacementDateObj);

        String status = determineStatus(remainingKm, remainingDays);

        int progressKm = calculateKmProgress(item, remainingKm);

        int progressDays = calculateDayProgress(item, remainingDays);

        return new MaintenanceItemDetailResponse(
                item.getName(),
                replacementCycleText,
                remainingKm != null ? remainingKm : 0L,
                lastReplacementDate,
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


    private String buildReplacementCycleText(MaintenanceItem item) {
        if (item.getReplacementCycle() != null && item.getReplacementKm() != null) {
            return String.format("매 %d km 또는 %d개월", item.getReplacementCycle(), item.getReplacementKm());
        } else if (item.getReplacementCycle() != null) {
            return String.format("매 %d km", item.getReplacementCycle());
        } else if (item.getReplacementKm() != null) {
            return String.format("매 %d개월", item.getReplacementKm());
        } else {
            return "상태 확인 필요";
        }
    }

    private Long calculateRemainingDays(MaintenanceItem item, LocalDate lastReplacementDate) {
        if (item.getReplacementCycle() == null || lastReplacementDate == null) {
            return Long.MAX_VALUE;
        }

        LocalDate nextReplacementDate = lastReplacementDate.plusMonths(item.getReplacementCycle());
        return ChronoUnit.DAYS.between(LocalDate.now(), nextReplacementDate);
    }

    private String determineStatus(Long remainingKm, Long remainingDays) {
        Long remainingKmValue = (remainingKm != null) ? remainingKm : Long.MAX_VALUE;

        if (remainingKmValue <= 0 || remainingDays <= 0) {
            return "교체 필요";
        } else if (remainingKmValue <= 5000 || remainingDays <= 30) {
            return "주의";
        } else {
            return "양호";
        }
    }

    private int calculateProgress(MaintenanceItem item, Long remainingKm, Long remainingDays) {
        Integer kmProgress = null;
        Integer dayProgress = null;

        if (item.getReplacementKm() != null && remainingKm != null) {
            double kmRatio = (double) remainingKm / item.getReplacementKm();
            kmProgress = (int) Math.max(0, Math.min(100, kmRatio * 100));
        }

        if (item.getReplacementCycle() != null && remainingDays != null && remainingDays != Long.MAX_VALUE) {
            long totalDays = item.getReplacementCycle() * 30L;
            double dayRatio = (double) remainingDays / totalDays;
            dayProgress = (int) Math.max(0, Math.min(100, dayRatio * 100));
        }

        if (kmProgress != null && dayProgress != null) {
            return Math.min(kmProgress, dayProgress);
        } else if (kmProgress != null) {
            return kmProgress;
        } else if (dayProgress != null) {
            return dayProgress;
        } else {
            return 0;
        }
    }

    private int calculateKmProgress(MaintenanceItem item, Long remainingKm) {
        Integer kmProgress = null;

        if (item.getReplacementKm() != null && remainingKm != null) {
            double kmRatio = (double) remainingKm / item.getReplacementKm();
            kmProgress = (int) Math.max(0, Math.min(100, kmRatio * 100));
        }

        return kmProgress;
    }

    private int calculateDayProgress(MaintenanceItem item, Long remainingDays) {
        Integer dayProgress = null;

        if (item.getReplacementCycle() != null && remainingDays != null && remainingDays != Long.MAX_VALUE) {
            long totalDays = item.getReplacementCycle() * 30L;
            double dayRatio = (double) remainingDays / totalDays;
            dayProgress = (int) Math.max(0, Math.min(100, dayRatio * 100));
        }

        return dayProgress;
    }


}