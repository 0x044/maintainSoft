package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@Entity
@RequiredArgsConstructor
@Table(name = "departments")
public class Department extends BaseEntity{

    @Column(nullable = false, unique = true)
    private String deptName;

    private String pocName;

    private Long pocNumber;
}
