package com.maintainsoft.controller;

import com.maintainsoft.dto.DepartmentRequest;
import com.maintainsoft.dto.DepartmentResponse;
import com.maintainsoft.service.DepartmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/")
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping("departments")
    ResponseEntity<List<DepartmentResponse>> listDepartments() {
        return ResponseEntity.status(HttpStatus.OK).body(departmentService.listDepartments());
    }

    @PostMapping("/department")
    ResponseEntity<DepartmentResponse> createDepartment(@RequestBody DepartmentRequest request){
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
