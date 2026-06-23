package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "repair_spares")
public class RepairSpare {
    @EmbeddedId
    private RepairSpareId id = new RepairSpareId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("repairId")
    @JoinColumn(name = "repair_id")
    private Repair repair;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("spareId")
    @JoinColumn(name = "spare_id")
    private Spare spare;

    @Column(nullable = false)
    private int usedQuantity;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RepairSpareId implements Serializable {
        private UUID repairId;
        private UUID spareId;
    }
}
