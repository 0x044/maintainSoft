package com.maintainsoft.entity;

import com.maintainsoft.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(name = "role")
    @Enumerated(EnumType.ORDINAL)
    private Role role;

    @Column(nullable = false, unique = true)
    private Long mobileNo;

    @ManyToOne
    private Department department;
}
