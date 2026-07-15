package com.gitforge.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Facade for opening the SQLite database and ensuring the schema exists.
 */
public final class DatabaseManager {

    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final DatabaseInitializer initializer = new DatabaseInitializer();
    private boolean initialized;

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public synchronized void initialize() throws SQLException {
        initialize(null);
    }

    public synchronized void initialize(Path databaseFile) throws SQLException {
        if (databaseFile == null) {
            connectionManager.openDefault();
        } else {
            connectionManager.open(databaseFile);
        }
        initializer.initialize(connectionManager.getConnection());
        initialized = true;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            initialize();
        }
        return connectionManager.getConnection();
    }

    public synchronized Path getDatabasePath() {
        return connectionManager.getDatabasePath();
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized void shutdown() throws SQLException {
        connectionManager.close();
        initialized = false;
    }

    public synchronized void initializeAt(Path databaseFile) throws SQLException {
        Objects.requireNonNull(databaseFile, "databaseFile");
        initialize(databaseFile);
    }
}
