package com.ll.carjini.domain.maintenanceItem.service;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.maintenanceItem.dto.MaintenanceAlarmEvent;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.maintenanceItem.repository.MaintenanceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceCheckService {

    private final MaintenanceItemRepository maintenanceItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 30 16 * * ?", zone = "Asia/Seoul")
    @Transactional(readOnly = true)
    public void checkMaintenanceItems() {
        List<MaintenanceItem> allItems = maintenanceItemRepository.findAll();

        LocalDate today = LocalDate.now();

        for (MaintenanceItem item : allItems) {
            if (item.isCycleAlarm() && item.getReplacementCycle() != null) {
                checkCycleAlarm(item, today);
            }

            if (item.isKmAlarm() && item.getReplacementKm() != null) {
                checkKmAlarm(item);
            }
        }
    }

    private void checkCycleAlarm(MaintenanceItem item, LocalDate today) {
        LocalDate lastMaintenanceDate = item.getMaintenanceHistories().stream()
                .map(history -> history.getReplacementDate())
                .max(LocalDate::compareTo)
                .orElse(item.getCarOwner().getStartDate());

        long daysSinceLastMaintenance = ChronoUnit.DAYS.between(lastMaintenanceDate, today);

        if (daysSinceLastMaintenance >= item.getReplacementCycle()) {

            eventPublisher.publishEvent(new MaintenanceAlarmEvent(
                    item,
                    "CYCLE",
                    daysSinceLastMaintenance,
                    item.getReplacementCycle()
            ));
        }
    }

    private void checkKmAlarm(MaintenanceItem item) {
        CarOwner carOwner = item.getCarOwner();
        Long currentKm = carOwner.getNowKm();

        Long lastMaintenanceKm = item.getMaintenanceHistories().stream()
                .map(history -> history.getReplacementKm())
                .max(Long::compareTo)
                .orElse(carOwner.getStartKm());

        Long kmSinceLastMaintenance = currentKm - lastMaintenanceKm;

        if (kmSinceLastMaintenance >= item.getReplacementKm()) {

            eventPublisher.publishEvent(new MaintenanceAlarmEvent(
                    item,
                    "KM",
                    kmSinceLastMaintenance,
                    item.getReplacementKm()
            ));
        }
    }
}