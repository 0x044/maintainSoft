package com.maintainsoft.controller;

import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.entity.Department;
import com.maintainsoft.service.DepartmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/")
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping("departments")
    ResponseEntity<List<DepartmentResponse>> listDepartments() {
        List<DepartmentResponse> list = new ArrayList<>();

        for (Department department : departmentService.listDepartments()){
            list.add(
                    new DepartmentResponse(department.getId(), department.getDeptName(), department.getPocName(), department.getPocNumber())
            );
        }

        return ResponseEntity.status(HttpStatus.OK).body(list);
    }

}
