package com.gitforge.service;

import com.gitforge.database.DatabaseManager;

import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Thin bootstrap service for the SQLite data layer.
 * Does not contain Git or UI business rules.
 */
public class DatabaseService {

    private final DatabaseManager databaseManager;

    public DatabaseService() {
        this(DatabaseManager.getInstance());
    }

    public DatabaseService(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void initializeDatabase() throws SQLException {
        databaseManager.initialize();
    }

    public void initializeDatabase(Path databaseFile) throws SQLException {
        databaseManager.initializeAt(databaseFile);
    }

    public Path getDatabasePath() {
        return databaseManager.getDatabasePath();
    }

    public void shutdown() throws SQLException {
        databaseManager.shutdown();
    }
}
