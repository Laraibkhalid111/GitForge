package com.gitforge.dao;

import com.gitforge.model.Settings;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the settings table.
 */
public class SettingsDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO settings (key, value, updated_at)
            VALUES (?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, key, value, updated_at
            FROM settings
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE settings
            SET key = ?, value = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM settings WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, key, value, updated_at
            FROM settings
            ORDER BY key COLLATE NOCASE
            """;

    private static final String SEARCH = """
            SELECT id, key, value, updated_at
            FROM settings
            WHERE key LIKE ? ESCAPE '\\'
               OR IFNULL(value, '') LIKE ? ESCAPE '\\'
            ORDER BY key COLLATE NOCASE
            """;

    private static final String SELECT_BY_KEY = """
            SELECT id, key, value, updated_at
            FROM settings
            WHERE key = ?
            """;

    public long create(Settings settings) throws SQLException {
        if (settings.getUpdatedAt() == null) {
            settings.setUpdatedAt(Instant.now());
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            statement.setString(1, settings.getKey());
            statement.setString(2, settings.getValue());
            statement.setString(3, InstantFormats.format(settings.getUpdatedAt()));
            long id = executeInsert(statement);
            settings.setId(id);
            return id;
        }
    }

    public Optional<Settings> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public Optional<Settings> findByKey(String key) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_KEY)) {
            statement.setString(1, key);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Settings settings) throws SQLException {
        if (settings.getId() == null) {
            throw new IllegalArgumentException("Settings id is required for update");
        }
        settings.setUpdatedAt(Instant.now());
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            statement.setString(1, settings.getKey());
            statement.setString(2, settings.getValue());
            statement.setString(3, InstantFormats.format(settings.getUpdatedAt()));
            statement.setLong(4, settings.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return executeUpdate(statement);
        }
    }

    public List<Settings> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Settings> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            return queryList(statement, this::mapRow);
        }
    }

    private Settings mapRow(ResultSet resultSet) throws SQLException {
        return new Settings(
                resultSet.getLong("id"),
                resultSet.getString("key"),
                resultSet.getString("value"),
                InstantFormats.parse(resultSet.getString("updated_at"))
        );
    }
}
