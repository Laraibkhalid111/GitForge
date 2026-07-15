package com.gitforge.dao;

import com.gitforge.model.Repository;
import com.gitforge.util.InstantFormats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the repositories table.
 */
public class RepositoryDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO repositories (name, path, description, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, name, path, description, created_at, updated_at
            FROM repositories
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE repositories
            SET name = ?, path = ?, description = ?, updated_at = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM repositories WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, name, path, description, created_at, updated_at
            FROM repositories
            ORDER BY name COLLATE NOCASE
            """;

    private static final String SEARCH = """
            SELECT id, name, path, description, created_at, updated_at
            FROM repositories
            WHERE name LIKE ? ESCAPE '\\'
               OR IFNULL(path, '') LIKE ? ESCAPE '\\'
               OR IFNULL(description, '') LIKE ? ESCAPE '\\'
            ORDER BY name COLLATE NOCASE
            """;

    private static final String SELECT_BY_NAME = """
            SELECT id, name, path, description, created_at, updated_at
            FROM repositories
            WHERE name = ? COLLATE NOCASE
            """;

    private static final String COUNT_BY_NAME = """
            SELECT COUNT(*)
            FROM repositories
            WHERE name = ? COLLATE NOCASE
            """;

    private static final String COUNT_BY_NAME_EXCLUDING = """
            SELECT COUNT(*)
            FROM repositories
            WHERE name = ? COLLATE NOCASE
              AND id <> ?
            """;

    public long create(Repository repository) throws SQLException {
        Instant now = Instant.now();
        if (repository.getCreatedAt() == null) {
            repository.setCreatedAt(now);
        }
        repository.setUpdatedAt(now);

        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            statement.setString(1, repository.getName());
            statement.setString(2, repository.getPath());
            statement.setString(3, repository.getDescription());
            statement.setString(4, InstantFormats.format(repository.getCreatedAt()));
            statement.setString(5, InstantFormats.format(repository.getUpdatedAt()));
            long id = executeInsert(statement);
            repository.setId(id);
            return id;
        }
    }

    public Optional<Repository> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Repository repository) throws SQLException {
        if (repository.getId() == null) {
            throw new IllegalArgumentException("Repository id is required for update");
        }
        repository.setUpdatedAt(Instant.now());
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            statement.setString(1, repository.getName());
            statement.setString(2, repository.getPath());
            statement.setString(3, repository.getDescription());
            statement.setString(4, InstantFormats.format(repository.getUpdatedAt()));
            statement.setLong(5, repository.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        Connection connection = connection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement(DELETE)) {
            statement.setLong(1, id);
            boolean deleted = statement.executeUpdate() > 0;
            connection.commit();
            return deleted;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public List<Repository> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Repository> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            return queryList(statement, this::mapRow);
        }
    }

    public Optional<Repository> findByNameIgnoreCase(String name) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_NAME)) {
            statement.setString(1, name);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean existsByNameIgnoreCase(String name, Long excludeId) throws SQLException {
        if (excludeId == null) {
            try (PreparedStatement statement = connection().prepareStatement(COUNT_BY_NAME)) {
                statement.setString(1, name);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() && resultSet.getInt(1) > 0;
                }
            }
        }
        try (PreparedStatement statement = connection().prepareStatement(COUNT_BY_NAME_EXCLUDING)) {
            statement.setString(1, name);
            statement.setLong(2, excludeId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public int countAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement("SELECT COUNT(*) FROM repositories");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private Repository mapRow(ResultSet resultSet) throws SQLException {
        return new Repository(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("path"),
                resultSet.getString("description"),
                InstantFormats.parse(resultSet.getString("created_at")),
                InstantFormats.parse(resultSet.getString("updated_at"))
        );
    }
}
