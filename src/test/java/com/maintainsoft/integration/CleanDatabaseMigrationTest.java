package com.maintainsoft.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that the migration chain builds the whole schema from nothing.
 *
 * <p>The test suite runs against an embedded database that is created empty for every
 * build, so this is genuine clean-database evidence rather than a re-run against a
 * long-lived environment. {@code ddl-auto=validate} is also active, which means the
 * JPA entities are checked against the migrated schema during context startup.
 */
@SpringBootTest
class CleanDatabaseMigrationTest {

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
                "select version, description, success from flyway_schema_history order by installed_rank");

        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
        assertThat(applied).extracting(row -> row.get("version").toString())
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
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
