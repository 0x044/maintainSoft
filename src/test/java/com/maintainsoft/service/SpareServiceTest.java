package com.maintainsoft.service;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.entity.Spare;
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

    private Spare spare(String name, String partNumber, int stock) {
        Spare spare = new Spare();
        spare.setName(name);
        spare.setPartNumber(partNumber);
        spare.setStock(stock);
        return spare;
    }
}
