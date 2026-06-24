package com.maintainsoft.security;

import com.maintainsoft.entity.Department;
import com.maintainsoft.entity.User;
import com.maintainsoft.enums.Role;
import com.maintainsoft.repository.DepartmentRepository;
import com.maintainsoft.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseInitializer Unit Tests")
class DatabaseInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DatabaseInitializer databaseInitializer;

    @Captor
    private ArgumentCaptor<Department> departmentCaptor;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Nested
    @DisplayName("When database is empty (count == 0)")
    class WhenDatabaseIsEmpty {

        @Test
        @DisplayName("Should initialize database with department and user")
        void run_initializesDatabaseWhenNoUsersExist() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert
            verify(departmentRepository, times(1)).save(any(Department.class));
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Should save department with correct values")
        void run_savesDepartmentWithCorrectValues() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert
            verify(departmentRepository).save(departmentCaptor.capture());
            Department savedDepartment = departmentCaptor.getValue();
            assertThat(savedDepartment.getDeptName()).isEqualTo("Administration");
            assertThat(savedDepartment.getPocName()).isEqualTo("Balasubramanian");
            assertThat(savedDepartment.getPocNumber()).isEqualTo(9842205227L);
        }

        @Test
        @DisplayName("Should save user with correct values")
        void run_savesUserWithCorrectValues() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            Department savedDepartment = new Department();
            savedDepartment.setDeptName("Administration");
            when(departmentRepository.save(any(Department.class))).thenReturn(savedDepartment);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getName()).isEqualTo("Balasubramanian");
            assertThat(savedUser.getEmail()).isEqualTo("balu@softex.com");
            assertThat(savedUser.getPhone()).isEqualTo("9842205227");
            assertThat(savedUser.getRole()).isEqualTo(Role.MANAGER);
            assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
            assertThat(savedUser.getDepartment()).isEqualTo(savedDepartment);
        }

        @Test
        @DisplayName("Should encode password before saving user")
        void run_encodesPasswordBeforeSaving() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("bcrypt-hashed-password");
            when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert
            verify(passwordEncoder).encode("password");
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("bcrypt-hashed-password");
        }

        @Test
        @DisplayName("Should save department before saving user")
        void run_savesDepartmentBeforeUser() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert — verify ordering
            var inOrder = inOrder(departmentRepository, userRepository);
            inOrder.verify(departmentRepository).save(any(Department.class));
            inOrder.verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should associate saved department with user")
        void run_associatesDepartmentWithUser() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(0L);
            when(passwordEncoder.encode("password")).thenReturn("encoded-password");
            Department returnedDept = new Department();
            returnedDept.setDeptName("Administration");
            returnedDept.setPocName("Balasubramanian");
            returnedDept.setPocNumber(9842205227L);
            when(departmentRepository.save(any(Department.class))).thenReturn(returnedDept);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            databaseInitializer.run();

            // Assert
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getDepartment()).isSameAs(returnedDept);
        }
    }

    @Nested
    @DisplayName("When database already has users (count > 0)")
    class WhenDatabaseHasUsers {

        @Test
        @DisplayName("Should not save anything when users already exist")
        void run_doesNothingWhenUsersExist() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(5L);

            // Act
            databaseInitializer.run();

            // Assert
            verify(userRepository).count();
            verify(departmentRepository, never()).save(any(Department.class));
            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should not save anything when exactly one user exists")
        void run_doesNothingWhenOneUserExists() throws Exception {
            // Arrange
            when(userRepository.count()).thenReturn(1L);

            // Act
            databaseInitializer.run();

            // Assert
            verify(departmentRepository, never()).save(any());
            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }
    }
}
