package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
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

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RepairSpareId that)) {
                return false;
            }
            return Objects.equals(repairId, that.repairId)
                    && Objects.equals(spareId, that.spareId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repairId, spareId);
        }
    }
}
