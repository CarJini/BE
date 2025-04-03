package com.ll.carjini.domain.maintenanceHistory.entity;
import com.ll.carjini.domain.carOwner.entity.CarOwner;
import com.ll.carjini.domain.maintenanceItem.entity.MaintenanceItem;
import com.ll.carjini.global.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@SuperBuilder
@Getter
@Setter
public class MaintenanceHistory extends BaseEntity {

    private LocalDate replacementDate;
    private Long replacementKm;

    @ManyToOne
    @JoinColumn(name = "maintenance_item_id", nullable = false)
    private MaintenanceItem maintenanceItem;

    @ManyToOne
    @JoinColumn(name = "car_owner_id", nullable = false)
    private CarOwner carOwner;

    @PrePersist
    public void setDefaultValues() {
        if (this.replacementDate == null && this.carOwner != null) {
            this.replacementDate = this.carOwner.getStartDate();
        }

        if (this.replacementKm == null && this.carOwner != null) {
            this.replacementKm = this.carOwner.getStartKm() + this.carOwner.getNowKm();
        }
    }
}
