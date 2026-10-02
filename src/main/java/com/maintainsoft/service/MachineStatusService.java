package com.maintainsoft.service;

import com.maintainsoft.dto.CreateMachineStatusRequest;
import com.maintainsoft.dto.MachineStatusResponse;
import com.maintainsoft.entity.MachineStatus;
import com.maintainsoft.repository.MachineStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@RequiredArgsConstructor
public class MachineStatusService {

    private final MachineStatusRepository machineStatusRepository;

    @Transactional(readOnly = true)
    public List<MachineStatusResponse> listStatuses() {
        return machineStatusRepository.findAllByOrderByBuiltInDescNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public MachineStatusResponse createCustomStatus(CreateMachineStatusRequest request) {
        MachineStatus status = new MachineStatus();
        status.setName(request.name());
        status.setColor(request.color());
        status.setBuiltIn(false);

        MachineStatus saved = machineStatusRepository.save(status);
        return toResponse(saved);
    }

    private MachineStatusResponse toResponse(MachineStatus status) {
        return new MachineStatusResponse(
                status.getId(),
                status.getName(),
                status.getColor(),
                status.isBuiltIn()
        );
    }
}
