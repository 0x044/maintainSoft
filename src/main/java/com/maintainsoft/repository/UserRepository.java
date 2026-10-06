package com.maintainsoft.repository;

import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    List<User> findAllByOrderByNameAsc();

    List<User> findAllByRoleAndDeletedFalse(Role role);
}
