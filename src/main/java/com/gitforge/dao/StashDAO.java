package com.gitforge.dao;

import com.gitforge.model.Stash;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the stash table.
 */
public class StashDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO stash (repository_id, name, message, created_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, repository_id, name, message, created_at
            FROM stash
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE stash
            SET repository_id = ?, name = ?, message = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM stash WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, repository_id, name, message, created_at
            FROM stash
            ORDER BY created_at DESC
            """;

    private static final String SEARCH = """
            SELECT id, repository_id, name, message, created_at
            FROM stash
            WHERE name LIKE ? ESCAPE '\\'
               OR IFNULL(message, '') LIKE ? ESCAPE '\\'
            ORDER BY created_at DESC
            """;

    public long create(Stash stash) throws SQLException {
        if (stash.getCreatedAt() == null) {
            stash.setCreatedAt(Instant.now());
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            statement.setLong(1, stash.getRepositoryId());
            statement.setString(2, stash.getName());
            statement.setString(3, stash.getMessage());
            statement.setString(4, InstantFormats.format(stash.getCreatedAt()));
            long id = executeInsert(statement);
            stash.setId(id);
            return id;
        }
    }

    public Optional<Stash> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Stash stash) throws SQLException {
        if (stash.getId() == null) {
            throw new IllegalArgumentException("Stash id is required for update");
        }
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            statement.setLong(1, stash.getRepositoryId());
            statement.setString(2, stash.getName());
            statement.setString(3, stash.getMessage());
            statement.setLong(4, stash.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return executeUpdate(statement);
        }
    }

    public List<Stash> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Stash> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            return queryList(statement, this::mapRow);
        }
    }

    private Stash mapRow(ResultSet resultSet) throws SQLException {
        return new Stash(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                resultSet.getString("name"),
                resultSet.getString("message"),
                InstantFormats.parse(resultSet.getString("created_at"))
        );
    }
}
