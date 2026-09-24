package com.maintainsoft.controller;

import com.maintainsoft.dto.CreateSpareRequest;
import com.maintainsoft.dto.SpareResponse;
import com.maintainsoft.dto.UpdateSpareRequest;
import com.maintainsoft.service.SpareService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SpareControllerTest {

    @Mock
    private SpareService spareService;

    @InjectMocks
    private SpareController spareController;

    @Test
    void listSparesReturnsOk() {
        when(spareService.listSpares()).thenReturn(List.of(response()));

        ResponseEntity<List<SpareResponse>> result = spareController.listSpares();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        verify(spareService).listSpares();
    }

    @Test
    void createSpareReturnsCreated() {
        CreateSpareRequest request = createRequest();
        when(spareService.createSpare(request)).thenReturn(response());

        ResponseEntity<SpareResponse> result = spareController.createSpare(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(spareService).createSpare(request);
    }

    @Test
    void updateSpareReturnsOk() {
        UUID id = UUID.randomUUID();
        UpdateSpareRequest request = new UpdateSpareRequest(
                "BRG-002", "Bearing 6203", "Updated", "each", "Lathe"
        );
        when(spareService.updateSpare(id, request)).thenReturn(response());

        ResponseEntity<SpareResponse> result = spareController.updateSpare(id, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(spareService).updateSpare(id, request);
    }

    @Test
    void archiveSpareReturnsNoContent() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = spareController.archiveSpare(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(spareService).archiveSpare(id);
    }

    @Test
    void invalidSpareInputIsRejectedAtControllerBoundary() throws Exception {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        try {
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(spareController)
                    .setValidator(validator)
                    .build();

            mockMvc.perform(post("/api/v1/spares")
                            .contentType("application/json")
                            .content("{\"partNumber\":\"\",\"name\":\"\",\"initialStock\":-1}"))
                    .andExpect(status().isBadRequest());
        } finally {
            validator.destroy();
        }

        verifyNoInteractions(spareService);
    }

    private CreateSpareRequest createRequest() {
        return new CreateSpareRequest("BRG-001", "Bearing", "Steel bearing", "piece", "CNC Mill", 4);
    }

    private SpareResponse response() {
        return new SpareResponse(UUID.randomUUID(), "BRG-001", "Bearing", "Steel bearing", "piece", "CNC Mill", 4);
    }
}
