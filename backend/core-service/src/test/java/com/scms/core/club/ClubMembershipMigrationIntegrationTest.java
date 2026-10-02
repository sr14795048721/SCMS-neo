package com.scms.core.club;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class ClubMembershipMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scms")
            .withUsername("scms")
            .withPassword("scms");

    @Test
    void migrateShouldFailWhenDuplicateMembershipsExist() throws Exception {
        String schema = newSchema("dup_memberships");
        createSchema(schema);
        flyway(schema, "19").migrate();

        try (Connection connection = openConnection(schema)) {
            long adminUserId = insertUser(connection, "admin_memberships", "admin_memberships@scms.local", "ADMIN");
            long studentUserId = insertUser(connection, "student_memberships", "student_memberships@scms.local", "STUDENT");
            long firstClubId = insertClub(connection, "Robotics Memberships", adminUserId);
            long secondClubId = insertClub(connection, "Music Memberships", adminUserId);

            insertMembership(connection, firstClubId, studentUserId);
            insertMembership(connection, secondClubId, studentUserId);
        }

        FlywayException exception = assertThrows(FlywayException.class, () -> flyway(schema, null).migrate());

        assertTrue(exception.getMessage().contains("Duplicate club_student_members rows found for the same student"));
    }

    @Test
    void migrateShouldFailWhenDuplicatePendingJoinRequestsExist() throws Exception {
        String schema = newSchema("dup_requests");
        createSchema(schema);
        flyway(schema, "19").migrate();

        try (Connection connection = openConnection(schema)) {
            long adminUserId = insertUser(connection, "admin_requests", "admin_requests@scms.local", "ADMIN");
            long studentUserId = insertUser(connection, "student_requests", "student_requests@scms.local", "STUDENT");
            long firstClubId = insertClub(connection, "Robotics Requests", adminUserId);
            long secondClubId = insertClub(connection, "Music Requests", adminUserId);

            insertPendingJoinRequest(connection, firstClubId, studentUserId);
            insertPendingJoinRequest(connection, secondClubId, studentUserId);
        }

        FlywayException exception = assertThrows(FlywayException.class, () -> flyway(schema, null).migrate());

        assertTrue(exception.getMessage().contains("Duplicate pending club_join_requests rows found for the same student"));
    }

    @Test
    void latestMigrationShouldEnforceGlobalStudentUniqueness() throws Exception {
        String schema = newSchema("latest_constraints");
        createSchema(schema);
        flyway(schema, null).migrate();

        try (Connection connection = openConnection(schema)) {
            long adminUserId = insertUser(connection, "admin_clean", "admin_clean@scms.local", "ADMIN");
            long studentUserId = insertUser(connection, "student_clean", "student_clean@scms.local", "STUDENT");
            long firstClubId = insertClub(connection, "Robotics Clean", adminUserId);
            long secondClubId = insertClub(connection, "Music Clean", adminUserId);

            insertMembership(connection, firstClubId, studentUserId);
            SQLException membershipException = assertThrows(SQLException.class, () -> insertMembership(connection, secondClubId, studentUserId));
            assertEquals("23505", membershipException.getSQLState());

            insertPendingJoinRequest(connection, firstClubId, studentUserId);
            SQLException joinRequestException = assertThrows(SQLException.class, () -> insertPendingJoinRequest(connection, secondClubId, studentUserId));
            assertEquals("23505", joinRequestException.getSQLState());
        }
    }

    private Flyway flyway(String schema, String targetVersion) {
        var configuration = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .defaultSchema(schema)
                .locations("classpath:db/migration");
        if (targetVersion != null) {
            configuration.target(targetVersion);
        }
        return configuration.load();
    }

    private void createSchema(String schema) throws SQLException {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
        }
    }

    private Connection openConnection(String schema) throws SQLException {
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema);
        }
        return connection;
    }

    private long insertUser(Connection connection, String username, String email, String role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into users (username, email, password_hash, role, enabled) values (?, ?, ?, ?, ?) returning id")) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, "encoded");
            statement.setString(4, role);
            statement.setBoolean(5, true);
            statement.execute();
            try (var resultSet = statement.getResultSet()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private long insertClub(Connection connection, String name, long createdBy) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into clubs (name, description, created_by) values (?, ?, ?) returning id")) {
            statement.setString(1, name);
            statement.setString(2, name + " description");
            statement.setLong(3, createdBy);
            statement.execute();
            try (var resultSet = statement.getResultSet()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    private void insertMembership(Connection connection, long clubId, long studentUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into club_student_members (club_id, student_user_id, role) values (?, ?, ?)")) {
            statement.setLong(1, clubId);
            statement.setLong(2, studentUserId);
            statement.setString(3, "MEMBER");
            statement.executeUpdate();
        }
    }

    private void insertPendingJoinRequest(Connection connection, long clubId, long studentUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into club_join_requests (club_id, student_user_id, reason, status) values (?, ?, ?, ?)")) {
            statement.setLong(1, clubId);
            statement.setLong(2, studentUserId);
            statement.setString(3, "join");
            statement.setString(4, "PENDING");
            statement.executeUpdate();
        }
    }

    private String newSchema(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
}
