package com.gitforge.dao;

import com.gitforge.model.Branch;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the branches table.
 */
public class BranchDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO branches (repository_id, name, is_active, created_at)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, repository_id, name, is_active, created_at
            FROM branches
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE branches
            SET repository_id = ?, name = ?, is_active = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM branches WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, repository_id, name, is_active, created_at
            FROM branches
            ORDER BY name COLLATE NOCASE
            """;

    private static final String SEARCH = """
            SELECT id, repository_id, name, is_active, created_at
            FROM branches
            WHERE name LIKE ? ESCAPE '\\'
            ORDER BY name COLLATE NOCASE
            """;

    private static final String SELECT_BY_REPOSITORY = """
            SELECT id, repository_id, name, is_active, created_at
            FROM branches
            WHERE repository_id = ?
            ORDER BY name COLLATE NOCASE
            """;

    private static final String SELECT_ACTIVE_BY_REPOSITORY = """
            SELECT id, repository_id, name, is_active, created_at
            FROM branches
            WHERE repository_id = ? AND is_active = 1
            LIMIT 1
            """;

    private static final String COUNT_BY_REPOSITORY = """
            SELECT COUNT(*)
            FROM branches
            WHERE repository_id = ?
            """;

    public long create(Branch branch) throws SQLException {
        if (branch.getCreatedAt() == null) {
            branch.setCreatedAt(Instant.now());
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            statement.setLong(1, branch.getRepositoryId());
            statement.setString(2, branch.getName());
            statement.setInt(3, branch.isActive() ? 1 : 0);
            statement.setString(4, InstantFormats.format(branch.getCreatedAt()));
            long id = executeInsert(statement);
            branch.setId(id);
            return id;
        }
    }

    public Optional<Branch> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Branch branch) throws SQLException {
        if (branch.getId() == null) {
            throw new IllegalArgumentException("Branch id is required for update");
        }
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            statement.setLong(1, branch.getRepositoryId());
            statement.setString(2, branch.getName());
            statement.setInt(3, branch.isActive() ? 1 : 0);
            statement.setLong(4, branch.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return executeUpdate(statement);
        }
    }

    public List<Branch> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Branch> search(String query) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, likePattern(query));
            return queryList(statement, this::mapRow);
        }
    }

    public List<Branch> findByRepositoryId(long repositoryId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_REPOSITORY)) {
            statement.setLong(1, repositoryId);
            return queryList(statement, this::mapRow);
        }
    }

    public Optional<Branch> findActiveByRepositoryId(long repositoryId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ACTIVE_BY_REPOSITORY)) {
            statement.setLong(1, repositoryId);
            return queryOne(statement, this::mapRow);
        }
    }

    public int countByRepositoryId(long repositoryId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(COUNT_BY_REPOSITORY)) {
            statement.setLong(1, repositoryId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private Branch mapRow(ResultSet resultSet) throws SQLException {
        return new Branch(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                resultSet.getString("name"),
                resultSet.getInt("is_active") == 1,
                InstantFormats.parse(resultSet.getString("created_at"))
        );
    }
}
