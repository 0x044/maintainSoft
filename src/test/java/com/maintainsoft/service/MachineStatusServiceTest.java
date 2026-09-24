package com.maintainsoft.service;

import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.MachineStatusResponse;
import com.maintainsoft.entity.MachineStatus;
import com.maintainsoft.repository.MachineStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineStatusServiceTest {

    @Mock
    private MachineStatusRepository machineStatusRepository;

    @InjectMocks
    private MachineStatusService machineStatusService;

    @Test
    void listsActiveStatusesInRepositoryOrder() {
        MachineStatus operational = status("Operational", "#22C55E", true);
        MachineStatus awaitingParts = status("Awaiting parts", "#8B5CF6", false);
        when(machineStatusRepository.findAllByOrderByBuiltInDescNameAsc())
                .thenReturn(List.of(operational, awaitingParts));

        List<MachineStatusResponse> result = machineStatusService.listStatuses();

        assertThat(result).extracting(MachineStatusResponse::name)
                .containsExactly("Operational", "Awaiting parts");
        assertThat(result.get(1).builtIn()).isFalse();
    }

    @Test
    void createsCustomStatusWithManagerInput() {
        CreateMachineStatusRequest request = new CreateMachineStatusRequest("Awaiting parts", "#8B5CF6");
        when(machineStatusRepository.save(any(MachineStatus.class))).thenAnswer(invocation -> {
            MachineStatus status = invocation.getArgument(0);
            status.setId(UUID.randomUUID());
            return status;
        });

        MachineStatusResponse result = machineStatusService.createCustomStatus(request);

        assertThat(result.name()).isEqualTo("Awaiting parts");
        assertThat(result.color()).isEqualTo("#8B5CF6");
        assertThat(result.builtIn()).isFalse();
    }

    @Test
    void allowsDuplicateCustomStatusNames() {
        CreateMachineStatusRequest first = new CreateMachineStatusRequest("Awaiting parts", "#8B5CF6");
        CreateMachineStatusRequest second = new CreateMachineStatusRequest("Awaiting parts", "#111111");
        when(machineStatusRepository.save(any(MachineStatus.class))).thenAnswer(invocation -> {
            MachineStatus status = invocation.getArgument(0);
            status.setId(UUID.randomUUID());
            return status;
        });

        assertThat(machineStatusService.createCustomStatus(first).name())
                .isEqualTo(machineStatusService.createCustomStatus(second).name());
    }

    private MachineStatus status(String name, String color, boolean builtIn) {
        MachineStatus status = new MachineStatus();
        status.setId(UUID.randomUUID());
        status.setName(name);
        status.setColor(color);
        status.setBuiltIn(builtIn);
        return status;
    }
}
