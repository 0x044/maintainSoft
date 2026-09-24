package com.maintainsoft.service;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.StockAdjustmentRequest;
import com.maintainsoft.dto.StockIssueRequest;
import com.maintainsoft.dto.StockQuantityRequest;
import com.maintainsoft.dto.StockReturnRequest;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.entity.Repair;
import com.maintainsoft.entity.RepairSpare;
import com.maintainsoft.entity.Spare;
import com.maintainsoft.repository.RepairRepository;
import com.maintainsoft.repository.RepairSpareRepository;
import com.maintainsoft.exception.DuplicateSpareException;
import com.maintainsoft.exception.InvalidSpareException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.SpareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpareServiceTest {

    @Mock
    private SpareRepository spareRepository;

    @Mock
    private RepairRepository repairRepository;

    @Mock
    private RepairSpareRepository repairSpareRepository;

    @InjectMocks
    private SpareService spareService;

    @Test
    void listsSparesInNameOrder() {
        when(spareRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                spare("Bearing", "BRG-001", 4),
                spare("Filter", "FLT-001", 2)
        ));

        List<SpareResponse> result = spareService.listSpares();

        assertThat(result).extracting(SpareResponse::name)
                .containsExactly("Bearing", "Filter");
    }

    @Test
    void createsSpareWithCatalogFieldsAndInitialStock() {
        CreateSpareRequest request = new CreateSpareRequest(
                "BRG-001", "Bearing", "Steel bearing", "piece", "CNC Mill", 4
        );
        when(spareRepository.saveAndFlush(any(Spare.class))).thenAnswer(invocation -> {
            Spare spare = invocation.getArgument(0);
            spare.setId(UUID.randomUUID());
            return spare;
        });

        SpareResponse response = spareService.createSpare(request);

        assertThat(response.partNumber()).isEqualTo("BRG-001");
        assertThat(response.description()).isEqualTo("Steel bearing");
        assertThat(response.compatibleMachine()).isEqualTo("CNC Mill");
        assertThat(response.stock()).isEqualTo(4);
    }

    @Test
    void rejectsNegativeInitialStock() {
        CreateSpareRequest request = new CreateSpareRequest(
                "BRG-001", "Bearing", null, null, null, -1
        );

        assertThatThrownBy(() -> spareService.createSpare(request))
                .isInstanceOf(InvalidSpareException.class);
        verify(spareRepository, never()).saveAndFlush(any(Spare.class));
    }

    @Test
    void translatesDuplicatePartNumber() {
        CreateSpareRequest request = new CreateSpareRequest(
                "BRG-001", "Bearing", null, null, null, 0
        );
        when(spareRepository.saveAndFlush(any(Spare.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> spareService.createSpare(request))
                .isInstanceOf(DuplicateSpareException.class);
    }

    @Test
    void updatesCatalogFieldsWithoutChangingStock() {
        UUID id = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 4);
        spare.setId(id);
        when(spareRepository.findById(id)).thenReturn(Optional.of(spare));
        when(spareRepository.saveAndFlush(spare)).thenReturn(spare);

        SpareResponse response = spareService.updateSpare(
                id, new UpdateSpareRequest("BRG-002", "Bearing 6203", "Updated", "each", "Lathe")
        );

        assertThat(response.partNumber()).isEqualTo("BRG-002");
        assertThat(response.stock()).isEqualTo(4);
    }

    @Test
    void archivesSpareByMarkingItDeleted() {
        UUID id = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 4);
        spare.setId(id);
        when(spareRepository.findById(id)).thenReturn(Optional.of(spare));

        spareService.archiveSpare(id);

        assertThat(spare.isDeleted()).isTrue();
        verify(spareRepository).save(spare);
        verify(spareRepository, never()).deleteById(any());
    }

    @Test
    void rejectsUnknownSpareOnRead() {
        UUID id = UUID.randomUUID();
        when(spareRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spareService.getSpare(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void receivesStockUsingLockedBalance() {
        UUID id = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 4);
        spare.setId(id);
        when(spareRepository.findByIdForUpdate(id)).thenReturn(Optional.of(spare));
        when(spareRepository.save(spare)).thenReturn(spare);

        SpareResponse response = spareService.receiveStock(id, new StockQuantityRequest(3));

        assertThat(response.stock()).isEqualTo(7);
    }

    @Test
    void adjustsStockToAnAbsoluteBalance() {
        UUID id = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 4);
        spare.setId(id);
        when(spareRepository.findByIdForUpdate(id)).thenReturn(Optional.of(spare));
        when(spareRepository.save(spare)).thenReturn(spare);

        SpareResponse response = spareService.adjustStock(id, new StockAdjustmentRequest(0));

        assertThat(response.stock()).isZero();
    }

    @Test
    void rejectsNegativeAbsoluteAdjustment() {
        assertThatThrownBy(() -> spareService.adjustStock(
                UUID.randomUUID(), new StockAdjustmentRequest(-1)
        )).isInstanceOf(InvalidSpareException.class);
    }

    @Test
    void issuesStockAndLinksItToRepair() {
        UUID spareId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 4);
        spare.setId(spareId);
        Repair repair = new Repair();
        repair.setId(repairId);
        when(spareRepository.findByIdForUpdate(spareId)).thenReturn(Optional.of(spare));
        when(repairRepository.findById(repairId)).thenReturn(Optional.of(repair));
        when(repairSpareRepository.findById(any())).thenReturn(Optional.empty());
        when(repairSpareRepository.save(any(RepairSpare.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(spareRepository.save(spare)).thenReturn(spare);

        SpareResponse response = spareService.issueStock(
                spareId, new StockIssueRequest(2, repairId)
        );

        assertThat(response.stock()).isEqualTo(2);
        verify(repairSpareRepository).save(any(RepairSpare.class));
    }

    @Test
    void rejectsIssuingMoreThanAvailableStock() {
        UUID spareId = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 1);
        spare.setId(spareId);
        when(spareRepository.findByIdForUpdate(spareId)).thenReturn(Optional.of(spare));

        assertThatThrownBy(() -> spareService.issueStock(
                spareId, new StockIssueRequest(2, UUID.randomUUID())
        )).isInstanceOf(InvalidSpareException.class);
    }

    @Test
    void returnsStockAndReducesRepairUsage() {
        UUID spareId = UUID.randomUUID();
        UUID repairId = UUID.randomUUID();
        Spare spare = spare("Bearing", "BRG-001", 1);
        spare.setId(spareId);
        Repair repair = new Repair();
        repair.setId(repairId);
        RepairSpare repairSpare = new RepairSpare();
        repairSpare.getId().setRepairId(repairId);
        repairSpare.getId().setSpareId(spareId);
        repairSpare.setUsedQuantity(2);
        when(spareRepository.findByIdForUpdate(spareId)).thenReturn(Optional.of(spare));
        when(repairRepository.findById(repairId)).thenReturn(Optional.of(repair));
        when(repairSpareRepository.findById(any())).thenReturn(Optional.of(repairSpare));
        when(repairSpareRepository.save(repairSpare)).thenReturn(repairSpare);
        when(spareRepository.save(spare)).thenReturn(spare);

        SpareResponse response = spareService.returnStock(
                spareId, new StockReturnRequest(1, repairId)
        );

        assertThat(response.stock()).isEqualTo(2);
        assertThat(repairSpare.getUsedQuantity()).isEqualTo(1);
    }

    private Spare spare(String name, String partNumber, int stock) {
        Spare spare = new Spare();
        spare.setName(name);
        spare.setPartNumber(partNumber);
        spare.setStock(stock);
        return spare;
    }
}
