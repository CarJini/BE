package com.ll.carjini.domain.maintenanceHistory.repository;

import com.ll.carjini.domain.maintenanceHistory.entity.MaintenanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceHistoryRepository extends JpaRepository<MaintenanceHistory, Long> {
    List<MaintenanceHistory> findByCarOwnerId(Long carOwnerId);
}
