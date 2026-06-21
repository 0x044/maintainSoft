package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "repair_spares")
public class RepairSpare extends BaseEntity{
    @EmbeddedId
    private RepairSpareId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repairId")
    @JoinColumn(name = "repair_id")
    private Repair repair;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("spare_id")
    private Spare spare;

    @Column(nullable = false)
    private int usedQuantity;

    @Embeddable
    public class RepairSpareId implements Serializable{
        private UUID repairId;
        private UUID spareId;
    }
}
