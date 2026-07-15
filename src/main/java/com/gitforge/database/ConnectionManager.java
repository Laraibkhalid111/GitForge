package com.gitforge.database;

import com.gitforge.util.DatabasePaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Manages the shared SQLite JDBC connection for GitForge.
 */
public final class ConnectionManager {

    private static final ConnectionManager INSTANCE = new ConnectionManager();

    private Path databasePath;
    private Connection connection;

    private ConnectionManager() {
    }

    public static ConnectionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void open(Path databaseFile) throws SQLException {
        Objects.requireNonNull(databaseFile, "databaseFile");
        closeQuietly();

        try {
            Files.createDirectories(databaseFile.getParent());
        } catch (IOException ex) {
            throw new SQLException("Unable to create database directory: " + databaseFile.getParent(), ex);
        }

        this.databasePath = databaseFile.toAbsolutePath().normalize();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databasePath);
        configureConnection(this.connection);
    }

    public synchronized void openDefault() throws SQLException {
        open(DatabasePaths.defaultDatabaseFile());
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            openDefault();
        }
        return connection;
    }

    public synchronized Path getDatabasePath() {
        return databasePath;
    }

    public synchronized boolean isOpen() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    public synchronized void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    private void closeQuietly() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Best-effort close before reopen.
            }
            connection = null;
        }
    }

    private static void configureConnection(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
    }
}
