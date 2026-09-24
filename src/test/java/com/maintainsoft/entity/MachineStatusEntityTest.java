package com.maintainsoft.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MachineStatusEntityTest {

    @Test
    void exposesStatusFields() {
        MachineStatus status = new MachineStatus();
        status.setName("Awaiting parts");
        status.setColor("#8B5CF6");
        status.setSystemKey(null);
        status.setBuiltIn(false);
        status.setDeleted(false);
        status.setVersion(1L);

        assertThat(status.getName()).isEqualTo("Awaiting parts");
        assertThat(status.getColor()).isEqualTo("#8B5CF6");
        assertThat(status.getSystemKey()).isNull();
        assertThat(status.isBuiltIn()).isFalse();
        assertThat(status.isDeleted()).isFalse();
        assertThat(status.getVersion()).isEqualTo(1L);
    }
}
