package com.maintainsoft.service;

import com.maintainsoft.dto.DeleteResponse;
import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.repository.DepartmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

        return new DepartmentResponse(department.getId(), department.getDeptName(), department.getPocName(), department.getPocNumber());
    }

    public DeleteResponse deleteDepartment(UUID departmentId){
        if(departmentRepository.existsById(departmentId)){
            departmentRepository.deleteById(departmentId);
            return new DeleteResponse("success", Instant.now());
        }else{
            return new DeleteResponse("Department not found", Instant.now());
        }
    }
}
