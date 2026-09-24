package com.maintainsoft.service;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.exception.DuplicateDepartmentException;
import com.maintainsoft.exception.ResourceNotFoundException;
import com.maintainsoft.repository.DepartmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest request) {
        Department department = new Department();
        apply(department, request);

        try {
            return toResponse(departmentRepository.saveAndFlush(department));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateDepartmentException("Department already exists");
        }
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        apply(department, request);

        try {
            return toResponse(departmentRepository.saveAndFlush(department));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateDepartmentException("Department already exists");
        }
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        department.setDeleted(true);
        departmentRepository.save(department);
    }

    private void apply(Department department, DepartmentRequest request) {
        department.setDeptName(request.deptName());
        department.setPocName(request.pocName());
        department.setPocNumber(request.pocNumber());
    }

    private DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getDeptName(),
                department.getPocName(),
                department.getPocNumber()
        );
    }
}
