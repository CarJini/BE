package com.ll.carjini.domain.maintenanceHistory.event;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.carOwner.repository.CarOwnerRepository;
import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import com.ll.carjini.domain.maintenanceHistory.repository.MaintenanceHistoryRepository;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.domain.maintenanceItem.repository.MaintenanceItemRepository;
import com.ll.carjini.global.error.ErrorCode;
import com.ll.carjini.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MaintenanceHistoryEventListener {

    private final CarOwnerRepository carOwnerRepository;
    private final MaintenanceItemRepository maintenanceItemRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;

    @EventListener
    @Transactional
    public void handleMaintenanceHistoryEvent(MaintenanceHistoryEvent event) {
        Long carOwnerId = event.getCarOwnerId();

        CarOwner carOwner = carOwnerRepository.findById(carOwnerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

        Long maxKm = carOwner.getStartKm() + carOwner.getNowKm();

        List<MaintenanceItem> maintenanceItems = maintenanceItemRepository.findByCarOwnerId(carOwnerId);

        for (MaintenanceItem item : maintenanceItems) {
            Optional<MaintenanceHistory> latestHistory = maintenanceHistoryRepository
                    .findTopByMaintenanceItemIdOrderByReplacementDateDescReplacementKmDesc(item.getId());

            if (latestHistory.isPresent() && latestHistory.get().getReplacementKm() != null
                    && latestHistory.get().getReplacementKm() > maxKm) {
                maxKm = latestHistory.get().getReplacementKm();
            }
        }

        carOwner.setNowKm(maxKm);
        carOwnerRepository.save(carOwner);

    }
}