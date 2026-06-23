package com.maintainsoft.service;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.repository.DepartmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> listDepartments() {
        var list = new ArrayList<DepartmentResponse>();
        for (Department department : departmentRepository.findAll()) {
            list.add(
                    new DepartmentResponse(department.getId(), department.getDeptName(), department.getPocName(), department.getPocNumber())
            );
        }
        return list;
    }

    public DepartmentResponse createDepartment(DepartmentRequest request){
        Department department = new Department();
        department.setDeptName(request.deptName());
        department.setPocName(request.pocName());
        department.setPocNumber(request.pocNumber());
        departmentRepository.save(department);

        DepartmentResponse response = new DepartmentResponse(department.getId(), department.getDeptName(), department.getPocName(), department.getPocNumber());

        return response;
    }
}
