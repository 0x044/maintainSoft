package com.maintainsoft.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@RequiredArgsConstructor
@Table(name = "departments",
    uniqueConstraints = @UniqueConstraint(columnNames = "deptName")
)
@SQLDelete(sql = "UPDATE departments set deleted = true where id = ? and version = ?")
@SQLRestriction("deleted = false")
public class Department extends BaseEntity{

    @Column(nullable = false, unique = true)
    private String deptName;

    @Column(nullable = false)
    private String pocName;

    @Column(nullable = false)
    private Long pocNumber;

    @Version
    private Long version;

    @Column(nullable = false)
    boolean deleted = false;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Machine> machineList = new ArrayList<>();
}
