package com.maintainsoft.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RepairSpareIdTest {

    @Test
    void equalIdsHaveEqualHashCodes() {
        UUID repairId = UUID.randomUUID();
        UUID spareId = UUID.randomUUID();
        RepairSpare.RepairSpareId first = id(repairId, spareId);
        RepairSpare.RepairSpareId second = id(repairId, spareId);
        RepairSpare.RepairSpareId different = id(repairId, UUID.randomUUID());

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(different);
    }

    private RepairSpare.RepairSpareId id(UUID repairId, UUID spareId) {
        RepairSpare.RepairSpareId id = new RepairSpare.RepairSpareId();
        id.setRepairId(repairId);
        id.setSpareId(spareId);
        return id;
    }
}
