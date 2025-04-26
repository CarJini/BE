package com.ll.carjini.domain.maintenanceItem.repository;

import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceItemRepository extends JpaRepository<MaintenanceItem, Long> {
    Page<MaintenanceItem> findByCarOwner(CarOwner carOwner, Pageable pageable);
}
