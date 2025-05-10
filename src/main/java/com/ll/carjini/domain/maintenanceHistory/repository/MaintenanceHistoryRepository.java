package com.ll.carjini.domain.maintenanceHistory.repository;

import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {

    List<MaintenanceHistory> findByMaintenanceItemId(Long maintenanceItemId);

    Optional<MaintenanceHistory> findTopByMaintenanceItemIdOrderByReplacementDateDescReplacementKmDesc(Long id);

    void deleteByMaintenanceItem(MaintenanceItem item);
}
