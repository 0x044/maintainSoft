package com.maintainsoft.security;

import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    @Transactional
    @NullMarked
    public void run(String... args) throws Exception {
//        log.info("--- Current Users in Database ---");
//        userRepository.findAll().forEach(u -> log.info("ID: {}, Email: {}, Name: {}, Role: {}", u.getId(), u.getEmail(),
//                u.getName(), u.getRole()));
//        log.info("---------------------------------");

        // Sync sequence in postgresql to prevent PK violation
        try {
            entityManager.createNativeQuery(
                    "SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 0) + 1, false)")
                    .getSingleResult();
            log.info("Successfully synced users_id_seq sequence.");
        } catch (Exception e) {
            log.warn("Failed to sync sequence: {}", e.getMessage());
        }

        String managerEmail = "manager@maintainsoft.com";
        if (userRepository.findByEmail(managerEmail).isEmpty()) {
            log.info("Seeding database with default MANAGER user: {}", managerEmail);
            User manager = new User();
            manager.setName("Manager Account");
            manager.setEmail(managerEmail);
            manager.setPassword(passwordEncoder.encode("password"));
            manager.setRole(Role.MANAGER);
            manager.setMobileNo(9876543210L);
            manager.setDepartment(null);
            userRepository.save(manager);
            log.info("Default MANAGER user successfully seeded.");
        } else {
            log.info("Default MANAGER user already exists. Skipping seeding.");
        }
    }
}
