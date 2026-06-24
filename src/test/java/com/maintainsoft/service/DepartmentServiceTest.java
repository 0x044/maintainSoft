package com.maintainsoft.service;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department createDepartment(UUID id, String deptName, String pocName, Long pocNumber) {
        Department dept = new Department();
        dept.setDeptName(deptName);
        dept.setPocName(pocName);
        dept.setPocNumber(pocNumber);
        // BaseEntity id is normally set by Hibernate; we use reflection or setter for testing
        dept.setId(id);
        return dept;
    }

    @Nested
    @DisplayName("listDepartments()")
    class ListDepartmentsTests {

        @Test
        @DisplayName("should return empty list when no departments exist")
        void listDepartments_Empty() {
            // Arrange
            when(departmentRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<DepartmentResponse> result = departmentService.listDepartments();

            // Assert
            assertThat(result).isNotNull().isEmpty();
            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("should return populated list when departments exist")
        void listDepartments_Populated() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            Department dept1 = createDepartment(id1, "Engineering", "Alice", 1111111111L);
            Department dept2 = createDepartment(id2, "Marketing", "Bob", 2222222222L);

            when(departmentRepository.findAll()).thenReturn(List.of(dept1, dept2));

            // Act
            List<DepartmentResponse> result = departmentService.listDepartments();

            // Assert
            assertThat(result).hasSize(2);

            DepartmentResponse resp1 = result.get(0);
            assertThat(resp1.deptId()).isEqualTo(id1);
            assertThat(resp1.name()).isEqualTo("Engineering");
            assertThat(resp1.pocName()).isEqualTo("Alice");
            assertThat(resp1.pocPhone()).isEqualTo(1111111111L);

            DepartmentResponse resp2 = result.get(1);
            assertThat(resp2.deptId()).isEqualTo(id2);
            assertThat(resp2.name()).isEqualTo("Marketing");
            assertThat(resp2.pocName()).isEqualTo("Bob");
            assertThat(resp2.pocPhone()).isEqualTo(2222222222L);

            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("should return single element list when one department exists")
        void listDepartments_SingleElement() {
            // Arrange
            UUID id = UUID.randomUUID();
            Department dept = createDepartment(id, "HR", "Charlie", 3333333333L);
            when(departmentRepository.findAll()).thenReturn(List.of(dept));

            // Act
            List<DepartmentResponse> result = departmentService.listDepartments();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("HR");
        }
    }

    @Nested
    @DisplayName("createDepartment()")
    class CreateDepartmentTests {

        @Test
        @DisplayName("should create department successfully and return response")
        void createDepartment_Success() {
            // Arrange
            DepartmentRequest request = new DepartmentRequest("Engineering", "Alice", 1111111111L);

            // Act
            DepartmentResponse response = departmentService.createDepartment(request);

            // Assert — verify the entity saved has the correct fields
            ArgumentCaptor<Department> deptCaptor = ArgumentCaptor.forClass(Department.class);
            verify(departmentRepository).save(deptCaptor.capture());

            Department savedDept = deptCaptor.getValue();
            assertThat(savedDept.getDeptName()).isEqualTo("Engineering");
            assertThat(savedDept.getPocName()).isEqualTo("Alice");
            assertThat(savedDept.getPocNumber()).isEqualTo(1111111111L);

            // Assert response maps back correctly
            // Note: id will be null since Hibernate isn't running, which is expected
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Engineering");
            assertThat(response.pocName()).isEqualTo("Alice");
            assertThat(response.pocPhone()).isEqualTo(1111111111L);
        }

        @Test
        @DisplayName("should map all request fields to the department entity")
        void createDepartment_FieldMapping() {
            // Arrange
            DepartmentRequest request = new DepartmentRequest("Quality Assurance", "Diana", 4444444444L);

            // Act
            DepartmentResponse response = departmentService.createDepartment(request);

            // Assert
            assertThat(response.name()).isEqualTo("Quality Assurance");
            assertThat(response.pocName()).isEqualTo("Diana");
            assertThat(response.pocPhone()).isEqualTo(4444444444L);

            verify(departmentRepository).save(any(Department.class));
        }
    }
}
