package com.maintainsoft.testsupport;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A single embedded PostgreSQL instance shared by every test in the JVM.
 *
 * <p>This keeps {@code mvn verify} self-contained: the suite needs neither Docker,
 * a running server, nor the Jasypt master password that protects the application
 * datasource. Because the database is created empty on every build, the default run
 * doubles as clean-database migration evidence.
 */
public final class EmbeddedDatabase {

    private static final String DATABASE = "maintainsoft";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "postgres";

    private static final Object LOCK = new Object();

    private static EmbeddedPostgres instance;

    private EmbeddedDatabase() {
    }

    /**
     * Starts the database if it is not already running.
     */
    public static EmbeddedDatabase start() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = startAndCreateDatabase();
            }
            return new EmbeddedDatabase();
        }
    }

    /**
     * Exposes the datasource through system properties, which take precedence over
     * {@code application.properties} in Spring's property resolution order.
     */
    public void registerAsSystemProperties() {
        System.setProperty("spring.datasource.url", jdbcUrl());
        System.setProperty("spring.datasource.username", USERNAME);
        System.setProperty("spring.datasource.password", PASSWORD);
    }

    /**
     * Registers the datasource with a Spring {@code DynamicPropertyRegistry} for
     * tests that prefer an explicit {@code @DynamicPropertySource} hook.
     */
    public void registerAsDynamicProperties(
            org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> jdbcUrl());
        registry.add("spring.datasource.username", () -> USERNAME);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://localhost:" + get().getPort() + "/" + DATABASE;
    }

    private static EmbeddedPostgres get() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = startAndCreateDatabase();
            }
            return instance;
        }
    }

    private static EmbeddedPostgres startAndCreateDatabase() {
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder().start();
            createDatabase(postgres);
            return postgres;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to start the embedded PostgreSQL instance used by the test suite", exception);
        }
    }

    private static void createDatabase(EmbeddedPostgres postgres) throws SQLException {
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("create database " + DATABASE);
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            if (instance != null) {
                try {
                    instance.close();
                } catch (Exception exception) {
                    // The JVM is shutting down; nothing useful can be done here.
                }
                instance = null;
            }
        }
    }
}
