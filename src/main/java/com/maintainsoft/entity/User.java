package com.maintainsoft.entity;

import com.maintainsoft.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity(name = "Users")
@Table(name = "Users", 
  uniqueConstraints = @UniqueConstraint(columnNames = "email"),
  indexes = @Index(name = "idx_user_email", columnList = "email")
)
@SQLDelete(sql = "UPDATE Users SET deleted = true WHERE id = ? and version = ?")
@SQLRestriction("deleted = false")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User extends BaseEntity{

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, unique = true)
    private String phone;
    
    @Column(nullable = false)
    private boolean deleted = false;

    @ManyToOne
    private Department department;

    @Version
    private Long version;
}
