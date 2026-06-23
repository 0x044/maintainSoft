package com.maintainsoft.service;

import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.repository.DepartmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public List<Department> listDepartments(){
        return  departmentRepository.findAll();
    }
}
