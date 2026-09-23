package com.maintainsoft.service;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.exception.DuplicateDepartmentException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        lenient().when(departmentRepository.saveAndFlush(any(Department.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("listDepartments()")
    class ListDepartmentsTests {

        @Test
        @DisplayName("should return empty list when no departments exist")
        void listDepartments_Empty() {
            when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

            List<DepartmentResponse> result = departmentService.listDepartments();

            assertThat(result).isEmpty();
            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("should map all department fields")
        void listDepartments_Populated() {
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            when(departmentRepository.findAll()).thenReturn(List.of(
                    department(firstId, "Engineering", "Alice", 1111111111L),
                    department(secondId, "Marketing", "Bob", 2222222222L)
            ));

            List<DepartmentResponse> result = departmentService.listDepartments();

            assertThat(result).extracting(DepartmentResponse::deptId)
                    .containsExactly(firstId, secondId);
            assertThat(result.get(0).name()).isEqualTo("Engineering");
            assertThat(result.get(1).pocPhone()).isEqualTo(2222222222L);
        }
    }

    @Nested
    @DisplayName("createDepartment()")
    class CreateDepartmentTests {

        @Test
        @DisplayName("should create department and return response")
        void createDepartment_Success() {
            DepartmentRequest request = request("Engineering", "Alice", 1111111111L);

            DepartmentResponse response = departmentService.createDepartment(request);

            ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
            verify(departmentRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getDeptName()).isEqualTo("Engineering");
            assertThat(captor.getValue().getPocName()).isEqualTo("Alice");
            assertThat(captor.getValue().getPocNumber()).isEqualTo(1111111111L);
            assertThat(response.name()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("should translate database uniqueness failures to duplicate exception")
        void createDepartment_Duplicate_ThrowsDomainException() {
            DepartmentRequest request = request("Engineering", "Alice", 1111111111L);
            when(departmentRepository.saveAndFlush(any(Department.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate"));

            assertThatThrownBy(() -> departmentService.createDepartment(request))
                    .isInstanceOf(DuplicateDepartmentException.class)
                    .hasMessage("Department already exists");
        }
    }

    @Nested
    @DisplayName("updateDepartment()")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("should update by id and allow rename")
        void updateDepartment_Success() {
            UUID id = UUID.randomUUID();
            Department existing = department(id, "Engineering", "Alice", 1111111111L);
            when(departmentRepository.findById(id)).thenReturn(Optional.of(existing));

            DepartmentResponse response = departmentService.updateDepartment(
                    id, request("Research", "Bob", 2222222222L)
            );

            assertThat(existing.getDeptName()).isEqualTo("Research");
            assertThat(existing.getPocName()).isEqualTo("Bob");
            assertThat(response.deptId()).isEqualTo(id);
            verify(departmentRepository).saveAndFlush(existing);
        }

        @Test
        @DisplayName("should reject unknown id")
        void updateDepartment_NotFound_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(departmentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> departmentService.updateDepartment(
                    id, request("Research", "Bob", 2222222222L)
            )).isInstanceOf(ResourceNotFoundException.class);
            verify(departmentRepository, never()).saveAndFlush(any(Department.class));
        }
    }

    @Nested
    @DisplayName("deleteDepartment()")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("should delete an existing department")
        void deleteDepartment_Success() {
            UUID id = UUID.randomUUID();
            when(departmentRepository.existsById(id)).thenReturn(true);

            departmentService.deleteDepartment(id);

            verify(departmentRepository).deleteById(id);
        }

        @Test
        @DisplayName("should reject unknown id")
        void deleteDepartment_NotFound_ThrowsException() {
            UUID id = UUID.randomUUID();
            when(departmentRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> departmentService.deleteDepartment(id))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(departmentRepository, never()).deleteById(any());
        }
    }

    private Department department(UUID id, String name, String pocName, Long pocNumber) {
        Department department = new Department();
        department.setId(id);
        department.setDeptName(name);
        department.setPocName(pocName);
        department.setPocNumber(pocNumber);
        return department;
    }

    private DepartmentRequest request(String name, String pocName, Long pocNumber) {
        return new DepartmentRequest(name, pocName, pocNumber);
    }
}
