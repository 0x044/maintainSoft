package com.maintainsoft.security;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.department-name}")
    private String departmentName;

    @Value("${app.bootstrap.poc-name}")
    private String pocName;

    @Value("${app.bootstrap.poc-number}")
    private long pocNumber;

    @Value("${app.bootstrap.manager-name}")
    private String managerName;

    @Value("${app.bootstrap.manager-email}")
    private String managerEmail;

    @Value("${app.bootstrap.manager-phone}")
    private String managerPhone;

    @Value("${app.bootstrap.manager-password}")
    private String managerPassword;

    @Override
    public void run(String @NonNull ... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing Database");

            Department department = new Department();
            department.setDeptName(departmentName);
            department.setPocName(pocName);
            department.setPocNumber(pocNumber);
            departmentRepository.save(department);

            User user = new User();
            user.setDepartment(department);
            user.setName(managerName);
            user.setEmail(managerEmail);
            user.setPhone(managerPhone);
            user.setRole(Role.MANAGER);
            user.setPassword(passwordEncoder.encode(managerPassword));
            userRepository.save(user);
            log.info("DB Populated");
        }
    }
}
