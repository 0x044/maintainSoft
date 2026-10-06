package com.maintainsoft.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the migration state of the database the application actually uses.
 *
 * <p>The suite runs against the configured PostgreSQL instance, which already carries the
 * schema. This is therefore a drift guard rather than a clean-database test: Flyway
 * validates every applied migration's checksum against the file on disk during startup,
 * so a rewritten migration would already have failed the context load. What this adds is
 * an explicit, readable statement of what has been applied.
 */
@SpringBootTest
class MigrationStateTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "departments",
            "machines",
            "machine_statuses",
            "refresh_tokens",
            "repair_costs",
            "repair_spares",
            "repair_updates",
            "repairs",
            "spares",
            "technicians",
            "users"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void everyMigrationIsAppliedSuccessfully() {
        List<Map<String, Object>> applied = jdbcTemplate.queryForList(
                "select version, description, checksum, success from flyway_schema_history "
                        + "order by installed_rank");

        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
        assertThat(applied).extracting(row -> row.get("version").toString())
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }

    /**
     * Every applied migration must have a checksum on record. A null checksum means the
     * row was baselined rather than migrated, which hides whether the file on disk ever
     * actually ran against this database.
     */
    @Test
    void everyAppliedMigrationHasAChecksumOnRecord() {
        List<Map<String, Object>> missingChecksum = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where checksum is null");

        assertThat(missingChecksum).isEmpty();
    }

    @Test
    void everyDomainTableExists() {
        List<String> tables = jdbcTemplate.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class);

        assertThat(tables).containsAll(EXPECTED_TABLES);
    }

    @Test
    void builtInMachineStatusesAreSeeded() {
        List<Map<String, Object>> statuses = jdbcTemplate.queryForList(
                "select system_key, name, built_in from machine_statuses order by system_key");

        assertThat(statuses).hasSize(5);
        assertThat(statuses).extracting(row -> row.get("system_key") + "=" + row.get("name"))
                .containsExactlyInAnyOrder(
                        "OPERATIONAL=Operational",
                        "IDLE=Idle",
                        "UNDER_MAINTENANCE=Under maintenance",
                        "FAULT=Fault",
                        "DECOMMISSIONED=Decommissioned");
        assertThat(statuses).allSatisfy(row -> assertThat(row.get("built_in")).isEqualTo(true));
    }

    @Test
    void domainCheckConstraintsArePresent() {
        List<String> constraints = jdbcTemplate.queryForList(
                "select conname from pg_constraint where contype = 'c' and connamespace = 'public'::regnamespace",
                String.class);

        assertThat(constraints).contains(
                "ck_spares_stock_nonnegative",
                "ck_repair_spares_used_quantity_nonnegative",
                "ck_repairs_end_after_start",
                "ck_repair_costs_amount",
                "ck_machines_lifecycle_dates",
                "ck_machine_statuses_color");
    }
}
