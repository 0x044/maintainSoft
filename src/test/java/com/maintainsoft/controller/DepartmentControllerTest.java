package com.maintainsoft.controller;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.service.DepartmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentController Unit Tests")
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    @Nested
    @DisplayName("GET /api/v1/departments")
    class ListDepartmentsTests {

        @Test
        @DisplayName("Should return OK (200) with list of departments")
        void listDepartments_returnsOkStatus_withDepartmentList() {
            // Arrange
            UUID deptId1 = UUID.randomUUID();
            UUID deptId2 = UUID.randomUUID();
            List<DepartmentResponse> departments = List.of(
                    new DepartmentResponse(deptId1, "Engineering", "Alice", 9876543210L),
                    new DepartmentResponse(deptId2, "HR", "Bob", 1234567890L)
            );
            when(departmentService.listDepartments()).thenReturn(departments);

            // Act
            ResponseEntity<List<DepartmentResponse>> result = departmentController.listDepartments();

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody()).hasSize(2);
            assertThat(result.getBody().get(0).deptId()).isEqualTo(deptId1);
            assertThat(result.getBody().get(0).name()).isEqualTo("Engineering");
            assertThat(result.getBody().get(0).pocName()).isEqualTo("Alice");
            assertThat(result.getBody().get(0).pocPhone()).isEqualTo(9876543210L);
            assertThat(result.getBody().get(1).deptId()).isEqualTo(deptId2);
            assertThat(result.getBody().get(1).name()).isEqualTo("HR");
            verify(departmentService, times(1)).listDepartments();
        }

        @Test
        @DisplayName("Should return OK (200) with empty list when no departments exist")
        void listDepartments_returnsOkStatus_withEmptyList() {
            // Arrange
            when(departmentService.listDepartments()).thenReturn(Collections.emptyList());

            // Act
            ResponseEntity<List<DepartmentResponse>> result = departmentController.listDepartments();

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody()).isEmpty();
            verify(departmentService).listDepartments();
        }

        @Test
        @DisplayName("Should delegate to DepartmentService exactly once")
        void listDepartments_delegatesToService() {
            // Arrange
            when(departmentService.listDepartments()).thenReturn(Collections.emptyList());

            // Act
            departmentController.listDepartments();

            // Assert
            verify(departmentService, times(1)).listDepartments();
            verifyNoMoreInteractions(departmentService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/department")
    class CreateDepartmentTests {

        @Test
        @DisplayName("Should return CREATED (201) status with department response")
        void createDepartment_returnsCreatedStatus_withResponse() {
            // Arrange
            DepartmentRequest request = new DepartmentRequest("Engineering", "Alice", 9876543210L);
            UUID deptId = UUID.randomUUID();
            DepartmentResponse expectedResponse = new DepartmentResponse(
                    deptId, "Engineering", "Alice", 9876543210L
            );
            when(departmentService.createDepartment(request)).thenReturn(expectedResponse);

            // Act
            ResponseEntity<DepartmentResponse> result = departmentController.createDepartment(request);

            // Assert
            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().deptId()).isEqualTo(deptId);
            assertThat(result.getBody().name()).isEqualTo("Engineering");
            assertThat(result.getBody().pocName()).isEqualTo("Alice");
            assertThat(result.getBody().pocPhone()).isEqualTo(9876543210L);
            verify(departmentService, times(1)).createDepartment(request);
        }

        @Test
        @DisplayName("Should propagate exception from DepartmentService")
        void createDepartment_whenServiceThrows_propagatesException() {
            // Arrange
            DepartmentRequest request = new DepartmentRequest("Engineering", "Alice", 9876543210L);
            when(departmentService.createDepartment(request))
                    .thenThrow(new RuntimeException("Department already exists"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> departmentController.createDepartment(request));
            verify(departmentService).createDepartment(request);
        }

        @Test
        @DisplayName("Should pass the exact request object to the service")
        void createDepartment_passesExactRequestToService() {
            // Arrange
            DepartmentRequest request = new DepartmentRequest("Sales", "Charlie", 5551234567L);
            UUID deptId = UUID.randomUUID();
            when(departmentService.createDepartment(request))
                    .thenReturn(new DepartmentResponse(deptId, "Sales", "Charlie", 5551234567L));

            // Act
            departmentController.createDepartment(request);

            // Assert
            verify(departmentService).createDepartment(request);
            verifyNoMoreInteractions(departmentService);
        }
    }
}
