package com.maintainsoft.entity;

import com.maintainsoft.enums.RepairStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "repair_updates",
        indexes = {
                @Index(name = "idx_repair_update_repair", columnList = "repair_id")
        }
)
@Getter
@Setter
public class RepairUpdate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_id", nullable = false)
    private Repair repair;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepairStatus repairStatus;
}
