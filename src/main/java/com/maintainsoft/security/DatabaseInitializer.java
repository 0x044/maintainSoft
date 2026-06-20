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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Component
public class DatabaseInitializer implements CommandLineRunner {
  private final UserRepository userRepository;
  private final DepartmentRepository departmentRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String @NonNull ... args) {
    if (userRepository.count() == 0) {
      log.info("Initializing Database...........");

      Department department = new Department();
      department.setDeptName("Administration");
      department.setPocName("Balasubramanian");
      department.setPocNumber(9842205227L);
      departmentRepository.save(department);

      User user = new User();
      user.setDepartment(department);
      user.setName("Balasubramanian");
      user.setEmail("balu@softex.com");
      user.setPhone("9842205227");
      user.setRole(Role.MANAGER);
      user.setPassword(passwordEncoder.encode("password"));
      userRepository.save(user);

      log.info("DB Populated");
    }
  }
}
