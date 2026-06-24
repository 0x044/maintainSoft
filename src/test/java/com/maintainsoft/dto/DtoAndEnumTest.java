package com.maintainsoft.dto;

import com.maintainsoft.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DtoAndEnumTest {

    // ══════════════════════════════════════════════
    //  DTO RECORD TESTS
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("AuthResponse")
    class AuthResponseTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            AuthResponse r = new AuthResponse("access-tok", "refresh-tok", "a@b.com", "MANAGER");

            assertThat(r.accessToken()).isEqualTo("access-tok");
            assertThat(r.refreshToken()).isEqualTo("refresh-tok");
            assertThat(r.email()).isEqualTo("a@b.com");
            assertThat(r.role()).isEqualTo("MANAGER");
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            AuthResponse r1 = new AuthResponse("a", "b", "c", "d");
            AuthResponse r2 = new AuthResponse("a", "b", "c", "d");

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different values")
        void equalsForDifferentValues() {
            AuthResponse r1 = new AuthResponse("a", "b", "c", "d");
            AuthResponse r2 = new AuthResponse("x", "b", "c", "d");

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            AuthResponse r1 = new AuthResponse("a", "b", "c", "d");
            AuthResponse r2 = new AuthResponse("a", "b", "c", "d");

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            AuthResponse r = new AuthResponse("tok", "ref", "e@f.com", "SUPERVISOR");

            String str = r.toString();
            assertThat(str).contains("tok");
            assertThat(str).contains("ref");
            assertThat(str).contains("e@f.com");
            assertThat(str).contains("SUPERVISOR");
        }

        @Test
        @DisplayName("should handle null fields")
        void handlesNulls() {
            AuthResponse r = new AuthResponse(null, null, null, null);

            assertThat(r.accessToken()).isNull();
            assertThat(r.refreshToken()).isNull();
            assertThat(r.email()).isNull();
            assertThat(r.role()).isNull();
        }
    }

    @Nested
    @DisplayName("DepartmentRequest")
    class DepartmentRequestTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            DepartmentRequest r = new DepartmentRequest("Engineering", "John", 1234567890L);

            assertThat(r.deptName()).isEqualTo("Engineering");
            assertThat(r.pocName()).isEqualTo("John");
            assertThat(r.pocNumber()).isEqualTo(1234567890L);
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            DepartmentRequest r1 = new DepartmentRequest("A", "B", 1L);
            DepartmentRequest r2 = new DepartmentRequest("A", "B", 1L);

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different values")
        void equalsForDifferentValues() {
            DepartmentRequest r1 = new DepartmentRequest("A", "B", 1L);
            DepartmentRequest r2 = new DepartmentRequest("A", "B", 2L);

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            DepartmentRequest r1 = new DepartmentRequest("A", "B", 1L);
            DepartmentRequest r2 = new DepartmentRequest("A", "B", 1L);

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            DepartmentRequest r = new DepartmentRequest("Dept", "Poc", 999L);

            assertThat(r.toString()).contains("Dept", "Poc", "999");
        }
    }

    @Nested
    @DisplayName("DepartmentResponse")
    class DepartmentResponseTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            UUID id = UUID.randomUUID();
            DepartmentResponse r = new DepartmentResponse(id, "Engineering", "Jane", 9876543210L);

            assertThat(r.deptId()).isEqualTo(id);
            assertThat(r.name()).isEqualTo("Engineering");
            assertThat(r.pocName()).isEqualTo("Jane");
            assertThat(r.pocPhone()).isEqualTo(9876543210L);
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            UUID id = UUID.randomUUID();
            DepartmentResponse r1 = new DepartmentResponse(id, "A", "B", 1L);
            DepartmentResponse r2 = new DepartmentResponse(id, "A", "B", 1L);

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different ids")
        void equalsForDifferentIds() {
            DepartmentResponse r1 = new DepartmentResponse(UUID.randomUUID(), "A", "B", 1L);
            DepartmentResponse r2 = new DepartmentResponse(UUID.randomUUID(), "A", "B", 1L);

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            UUID id = UUID.randomUUID();
            DepartmentResponse r1 = new DepartmentResponse(id, "A", "B", 1L);
            DepartmentResponse r2 = new DepartmentResponse(id, "A", "B", 1L);

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            UUID id = UUID.randomUUID();
            DepartmentResponse r = new DepartmentResponse(id, "Dept", "Poc", 555L);

            assertThat(r.toString()).contains(id.toString(), "Dept", "Poc", "555");
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            Instant now = Instant.now();
            ErrorResponse r = new ErrorResponse(404, "Not Found", "Resource missing", now);

            assertThat(r.status()).isEqualTo(404);
            assertThat(r.error()).isEqualTo("Not Found");
            assertThat(r.message()).isEqualTo("Resource missing");
            assertThat(r.timeStamp()).isEqualTo(now);
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            Instant ts = Instant.parse("2026-01-01T00:00:00Z");
            ErrorResponse r1 = new ErrorResponse(500, "Error", "msg", ts);
            ErrorResponse r2 = new ErrorResponse(500, "Error", "msg", ts);

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different status")
        void equalsForDifferentStatus() {
            Instant ts = Instant.parse("2026-01-01T00:00:00Z");
            ErrorResponse r1 = new ErrorResponse(400, "Error", "msg", ts);
            ErrorResponse r2 = new ErrorResponse(500, "Error", "msg", ts);

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            Instant ts = Instant.parse("2026-01-01T00:00:00Z");
            ErrorResponse r1 = new ErrorResponse(409, "Conflict", "dup", ts);
            ErrorResponse r2 = new ErrorResponse(409, "Conflict", "dup", ts);

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            Instant ts = Instant.parse("2026-06-01T12:00:00Z");
            ErrorResponse r = new ErrorResponse(401, "Unauthorized", "bad token", ts);

            String str = r.toString();
            assertThat(str).contains("401", "Unauthorized", "bad token");
        }
    }

    @Nested
    @DisplayName("LoginRequest")
    class LoginRequestTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            LoginRequest r = new LoginRequest("user@test.com", "s3cret");

            assertThat(r.email()).isEqualTo("user@test.com");
            assertThat(r.password()).isEqualTo("s3cret");
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            LoginRequest r1 = new LoginRequest("a@b.com", "pass");
            LoginRequest r2 = new LoginRequest("a@b.com", "pass");

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different password")
        void equalsForDifferentPassword() {
            LoginRequest r1 = new LoginRequest("a@b.com", "pass1");
            LoginRequest r2 = new LoginRequest("a@b.com", "pass2");

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            LoginRequest r1 = new LoginRequest("a@b.com", "pass");
            LoginRequest r2 = new LoginRequest("a@b.com", "pass");

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            LoginRequest r = new LoginRequest("user@test.com", "password");

            assertThat(r.toString()).contains("user@test.com", "password");
        }
    }

    @Nested
    @DisplayName("RefreshRequest")
    class RefreshRequestTests {

        @Test
        @DisplayName("should create and access refreshToken")
        void accessors() {
            RefreshRequest r = new RefreshRequest("my-refresh-token");

            assertThat(r.refreshToken()).isEqualTo("my-refresh-token");
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            RefreshRequest r1 = new RefreshRequest("tok");
            RefreshRequest r2 = new RefreshRequest("tok");

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different values")
        void equalsForDifferentValues() {
            RefreshRequest r1 = new RefreshRequest("tok1");
            RefreshRequest r2 = new RefreshRequest("tok2");

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            RefreshRequest r1 = new RefreshRequest("tok");
            RefreshRequest r2 = new RefreshRequest("tok");

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field value")
        void toStringContainsFields() {
            RefreshRequest r = new RefreshRequest("my-token-123");

            assertThat(r.toString()).contains("my-token-123");
        }

        @Test
        @DisplayName("should handle null refreshToken")
        void handlesNull() {
            RefreshRequest r = new RefreshRequest(null);

            assertThat(r.refreshToken()).isNull();
        }
    }

    @Nested
    @DisplayName("RegisterRequest")
    class RegisterRequestTests {

        @Test
        @DisplayName("should create and access all fields")
        void accessors() {
            UUID deptId = UUID.randomUUID();
            RegisterRequest r = new RegisterRequest("Alice", "alice@co.com", "pass", "555-0001", deptId);

            assertThat(r.name()).isEqualTo("Alice");
            assertThat(r.email()).isEqualTo("alice@co.com");
            assertThat(r.password()).isEqualTo("pass");
            assertThat(r.phone()).isEqualTo("555-0001");
            assertThat(r.department()).isEqualTo(deptId);
        }

        @Test
        @DisplayName("equals should be true for same values")
        void equalsForSameValues() {
            UUID deptId = UUID.randomUUID();
            RegisterRequest r1 = new RegisterRequest("A", "a@b.com", "p", "1", deptId);
            RegisterRequest r2 = new RegisterRequest("A", "a@b.com", "p", "1", deptId);

            assertThat(r1).isEqualTo(r2);
        }

        @Test
        @DisplayName("equals should be false for different values")
        void equalsForDifferentValues() {
            UUID deptId = UUID.randomUUID();
            RegisterRequest r1 = new RegisterRequest("A", "a@b.com", "p", "1", deptId);
            RegisterRequest r2 = new RegisterRequest("B", "a@b.com", "p", "1", deptId);

            assertThat(r1).isNotEqualTo(r2);
        }

        @Test
        @DisplayName("hashCode should be equal for same values")
        void hashCodeConsistency() {
            UUID deptId = UUID.randomUUID();
            RegisterRequest r1 = new RegisterRequest("A", "a@b.com", "p", "1", deptId);
            RegisterRequest r2 = new RegisterRequest("A", "a@b.com", "p", "1", deptId);

            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }

        @Test
        @DisplayName("toString should contain field values")
        void toStringContainsFields() {
            UUID deptId = UUID.randomUUID();
            RegisterRequest r = new RegisterRequest("Bob", "bob@co.com", "secret", "555", deptId);

            String str = r.toString();
            assertThat(str).contains("Bob", "bob@co.com", "secret", "555", deptId.toString());
        }

        @Test
        @DisplayName("should handle null department UUID")
        void handlesNullDepartment() {
            RegisterRequest r = new RegisterRequest("A", "a@b.com", "p", "1", null);

            assertThat(r.department()).isNull();
        }
    }

    // ══════════════════════════════════════════════
    //  ENUM TESTS
    // ══════════════════════════════════════════════

    @Nested
    @DisplayName("OperationCondition")
    class OperationConditionTests {

        @Test
        @DisplayName("should have exactly 3 values")
        void valuesLength() {
            assertThat(OperationCondition.values()).hasSize(3);
        }

        @Test
        @DisplayName("should contain OPERATIONAL")
        void containsOperational() {
            assertThat(OperationCondition.valueOf("OPERATIONAL")).isEqualTo(OperationCondition.OPERATIONAL);
        }

        @Test
        @DisplayName("should contain UNDER_MAINTENANCE")
        void containsUnderMaintenance() {
            assertThat(OperationCondition.valueOf("UNDER_MAINTENANCE")).isEqualTo(OperationCondition.UNDER_MAINTENANCE);
        }

        @Test
        @DisplayName("should contain DECOMMISSIONED")
        void containsDecommissioned() {
            assertThat(OperationCondition.valueOf("DECOMMISSIONED")).isEqualTo(OperationCondition.DECOMMISSIONED);
        }

        @Test
        @DisplayName("valueOf should throw for invalid name")
        void valueOfThrowsForInvalid() {
            assertThatThrownBy(() -> OperationCondition.valueOf("INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values should contain all enum constants")
        void valuesContainsAll() {
            assertThat(OperationCondition.values()).containsExactly(
                    OperationCondition.OPERATIONAL,
                    OperationCondition.UNDER_MAINTENANCE,
                    OperationCondition.DECOMMISSIONED
            );
        }
    }

    @Nested
    @DisplayName("RepairPriority")
    class RepairPriorityTests {

        @Test
        @DisplayName("should have exactly 3 values")
        void valuesLength() {
            assertThat(RepairPriority.values()).hasSize(3);
        }

        @Test
        @DisplayName("should contain HIGH")
        void containsHigh() {
            assertThat(RepairPriority.valueOf("HIGH")).isEqualTo(RepairPriority.HIGH);
        }

        @Test
        @DisplayName("should contain LOW")
        void containsLow() {
            assertThat(RepairPriority.valueOf("LOW")).isEqualTo(RepairPriority.LOW);
        }

        @Test
        @DisplayName("should contain NORMAL")
        void containsNormal() {
            assertThat(RepairPriority.valueOf("NORMAL")).isEqualTo(RepairPriority.NORMAL);
        }

        @Test
        @DisplayName("valueOf should throw for invalid name")
        void valueOfThrowsForInvalid() {
            assertThatThrownBy(() -> RepairPriority.valueOf("URGENT"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values should contain all enum constants")
        void valuesContainsAll() {
            assertThat(RepairPriority.values()).containsExactly(
                    RepairPriority.HIGH,
                    RepairPriority.LOW,
                    RepairPriority.NORMAL
            );
        }
    }

    @Nested
    @DisplayName("RepairStatus")
    class RepairStatusTests {

        @Test
        @DisplayName("should have exactly 3 values")
        void valuesLength() {
            assertThat(RepairStatus.values()).hasSize(3);
        }

        @Test
        @DisplayName("should contain OPEN")
        void containsOpen() {
            assertThat(RepairStatus.valueOf("OPEN")).isEqualTo(RepairStatus.OPEN);
        }

        @Test
        @DisplayName("should contain IN_PROGRESS")
        void containsInProgress() {
            assertThat(RepairStatus.valueOf("IN_PROGRESS")).isEqualTo(RepairStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should contain COMPLETED")
        void containsCompleted() {
            assertThat(RepairStatus.valueOf("COMPLETED")).isEqualTo(RepairStatus.COMPLETED);
        }

        @Test
        @DisplayName("valueOf should throw for invalid name")
        void valueOfThrowsForInvalid() {
            assertThatThrownBy(() -> RepairStatus.valueOf("CLOSED"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values should contain all enum constants")
        void valuesContainsAll() {
            assertThat(RepairStatus.values()).containsExactly(
                    RepairStatus.OPEN,
                    RepairStatus.IN_PROGRESS,
                    RepairStatus.COMPLETED
            );
        }
    }

    @Nested
    @DisplayName("RepairType")
    class RepairTypeTests {

        @Test
        @DisplayName("should have exactly 3 values")
        void valuesLength() {
            assertThat(RepairType.values()).hasSize(3);
        }

        @Test
        @DisplayName("should contain PREVENTIVE")
        void containsPreventive() {
            assertThat(RepairType.valueOf("PREVENTIVE")).isEqualTo(RepairType.PREVENTIVE);
        }

        @Test
        @DisplayName("should contain BREAKDOWN")
        void containsBreakdown() {
            assertThat(RepairType.valueOf("BREAKDOWN")).isEqualTo(RepairType.BREAKDOWN);
        }

        @Test
        @DisplayName("should contain SCHEDULED")
        void containsScheduled() {
            assertThat(RepairType.valueOf("SCHEDULED")).isEqualTo(RepairType.SCHEDULED);
        }

        @Test
        @DisplayName("valueOf should throw for invalid name")
        void valueOfThrowsForInvalid() {
            assertThatThrownBy(() -> RepairType.valueOf("EMERGENCY"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values should contain all enum constants")
        void valuesContainsAll() {
            assertThat(RepairType.values()).containsExactly(
                    RepairType.PREVENTIVE,
                    RepairType.BREAKDOWN,
                    RepairType.SCHEDULED
            );
        }
    }

    @Nested
    @DisplayName("Role")
    class RoleTests {

        @Test
        @DisplayName("should have exactly 2 values")
        void valuesLength() {
            assertThat(Role.values()).hasSize(2);
        }

        @Test
        @DisplayName("should contain MANAGER")
        void containsManager() {
            assertThat(Role.valueOf("MANAGER")).isEqualTo(Role.MANAGER);
        }

        @Test
        @DisplayName("should contain SUPERVISOR")
        void containsSupervisor() {
            assertThat(Role.valueOf("SUPERVISOR")).isEqualTo(Role.SUPERVISOR);
        }

        @Test
        @DisplayName("valueOf should throw for invalid name")
        void valueOfThrowsForInvalid() {
            assertThatThrownBy(() -> Role.valueOf("ADMIN"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("values should contain all enum constants")
        void valuesContainsAll() {
            assertThat(Role.values()).containsExactly(
                    Role.MANAGER,
                    Role.SUPERVISOR
            );
        }
    }
}
