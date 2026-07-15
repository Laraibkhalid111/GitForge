package com.gitforge.database;

import com.gitforge.util.InstantFormats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the GitForge SQLite schema on first launch.
 */
public final class DatabaseInitializer {

    private static final String SEED_SETTING = """
            INSERT OR IGNORE INTO settings (key, value, updated_at)
            VALUES (?, ?, ?)
            """;

    public void initialize(Connection connection) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                createRepositoriesTable(statement);
                createBranchesTable(statement);
                createCommitsTable(statement);
                createMergesTable(statement);
                createStashTable(statement);
                createSettingsTable(statement);
                createIndexes(statement);
                migrateCommitsTable(connection);
                migrateBranchesTable(connection);
                migrateMergesTable(connection);
            }
            seedDefaultSettings(connection);
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static void createRepositoriesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS repositories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    path TEXT,
                    description TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
    }

    private static void createBranchesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS branches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,
                    parent_branch_id INTEGER,
                    description TEXT,
                    latest_commit TEXT,
                    status TEXT,
                    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
                    FOREIGN KEY (parent_branch_id) REFERENCES branches(id) ON DELETE SET NULL,
                    UNIQUE (repository_id, name)
                )
                """);
    }

    private static void migrateBranchesTable(Connection connection) throws SQLException {
        if (!columnExists(connection, "branches", "parent_branch_id")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE branches ADD COLUMN parent_branch_id INTEGER");
            }
        }
        if (!columnExists(connection, "branches", "description")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE branches ADD COLUMN description TEXT");
            }
        }
        if (!columnExists(connection, "branches", "latest_commit")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE branches ADD COLUMN latest_commit TEXT");
            }
        }
        if (!columnExists(connection, "branches", "status")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE branches ADD COLUMN status TEXT");
            }
        }
    }

    private static void createCommitsTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS commits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_id INTEGER NOT NULL,
                    branch_id INTEGER,
                    hash TEXT NOT NULL,
                    message TEXT,
                    author TEXT,
                    committed_at TEXT NOT NULL,
                    parent_hash TEXT,
                    commit_type TEXT,
                    files_changed INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
                    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL,
                    UNIQUE (repository_id, hash)
                )
                """);
    }

    private static void migrateCommitsTable(Connection connection) throws SQLException {
        if (!columnExists(connection, "commits", "commit_type")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE commits ADD COLUMN commit_type TEXT");
            }
        }
        if (!columnExists(connection, "commits", "files_changed")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE commits ADD COLUMN files_changed INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void createMergesTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS merges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_id INTEGER NOT NULL,
                    source_branch_id INTEGER,
                    target_branch_id INTEGER,
                    status TEXT NOT NULL,
                    message TEXT,
                    merged_at TEXT NOT NULL,
                    strategy TEXT,
                    merge_commit TEXT,
                    conflict_status TEXT,
                    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
                    FOREIGN KEY (source_branch_id) REFERENCES branches(id) ON DELETE SET NULL,
                    FOREIGN KEY (target_branch_id) REFERENCES branches(id) ON DELETE SET NULL
                )
                """);
    }

    private static void migrateMergesTable(Connection connection) throws SQLException {
        if (!columnExists(connection, "merges", "strategy")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE merges ADD COLUMN strategy TEXT");
            }
        }
        if (!columnExists(connection, "merges", "merge_commit")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE merges ADD COLUMN merge_commit TEXT");
            }
        }
        if (!columnExists(connection, "merges", "conflict_status")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE merges ADD COLUMN conflict_status TEXT");
            }
        }
    }

    private static void createStashTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS stash (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    repository_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    message TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE
                )
                """);
    }

    private static void createSettingsTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    key TEXT NOT NULL UNIQUE,
                    value TEXT,
                    updated_at TEXT NOT NULL
                )
                """);
    }

    private static void createIndexes(Statement statement) throws SQLException {
        statement.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_repositories_name ON repositories(name COLLATE NOCASE)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_branches_repository_id ON branches(repository_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_commits_repository_id ON commits(repository_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_commits_branch_id ON commits(branch_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_merges_repository_id ON merges(repository_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_stash_repository_id ON stash(repository_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(key)");
    }

    private static void seedDefaultSettings(Connection connection) throws SQLException {
        String now = InstantFormats.now();
        try (PreparedStatement statement = connection.prepareStatement(SEED_SETTING)) {
            insertSetting(statement, "theme", "dark", now);
            insertSetting(statement, "app.version", "1.0.0-SNAPSHOT", now);
        }
    }

    private static void insertSetting(PreparedStatement statement, String key, String value, String updatedAt)
            throws SQLException {
        statement.setString(1, key);
        statement.setString(2, value);
        statement.setString(3, updatedAt);
        statement.executeUpdate();
    }
}
