package com.gitforge.database;

import com.gitforge.dao.SettingsDAO;
import com.gitforge.model.Settings;
import com.gitforge.service.DatabaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseInitializerTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private Path databaseFile;

    @BeforeEach
    void setUp() throws Exception {
        databaseFile = tempDir.resolve("gitforge-test.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void createsDatabaseFileAndRequiredTables() throws Exception {
        assertTrue(Files.exists(databaseFile));

        Set<String> tables = new HashSet<>();
        Connection connection = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = ? ORDER BY name")) {
            statement.setString(1, "table");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString("name"));
                }
            }
        }

        assertTrue(tables.contains("repositories"));
        assertTrue(tables.contains("branches"));
        assertTrue(tables.contains("commits"));
        assertTrue(tables.contains("merges"));
        assertTrue(tables.contains("stash"));
        assertTrue(tables.contains("settings"));
    }

    @Test
    void seedsDefaultSettings() throws Exception {
        SettingsDAO settingsDAO = new SettingsDAO();
        List<Settings> settings = settingsDAO.findAll();
        assertTrue(settings.size() >= 2);

        Settings theme = settingsDAO.findByKey("theme").orElseThrow();
        assertEquals("dark", theme.getValue());

        Settings version = settingsDAO.findByKey("app.version").orElseThrow();
        assertEquals("1.0.0-SNAPSHOT", version.getValue());
    }
}
