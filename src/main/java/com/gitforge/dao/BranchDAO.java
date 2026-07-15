package com.gitforge.dao;

import com.gitforge.model.Branch;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the branches table.
 */
public class BranchDAO extends AbstractDAO {

    private static final String COLUMNS =
            "id, repository_id, name, is_active, created_at, parent_branch_id, description, "
                    + "latest_commit, status";

    private static final String INSERT =
            "INSERT INTO branches (repository_id, name, is_active, created_at, parent_branch_id, "
                    + "description, latest_commit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM branches WHERE id = ?";

    private static final String UPDATE =
            "UPDATE branches SET repository_id = ?, name = ?, is_active = ?, parent_branch_id = ?, "
                    + "description = ?, latest_commit = ?, status = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM branches WHERE id = ?";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM branches ORDER BY name COLLATE NOCASE";

    private static final String SEARCH =
            "SELECT " + COLUMNS + " FROM branches WHERE "
                    + "name LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(status, '') = ? COLLATE NOCASE "
                    + "ORDER BY name COLLATE NOCASE";

    private static final String SEARCH_BY_REPOSITORY =
            "SELECT " + COLUMNS + " FROM branches WHERE repository_id = ? AND ("
                    + "name LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(status, '') = ? COLLATE NOCASE) "
                    + "ORDER BY name COLLATE NOCASE";

    private static final String SELECT_BY_REPOSITORY =
            "SELECT " + COLUMNS + " FROM branches WHERE repository_id = ? ORDER BY name COLLATE NOCASE";

    private static final String SELECT_ACTIVE_BY_REPOSITORY =
            "SELECT " + COLUMNS + " FROM branches WHERE repository_id = ? AND is_active = 1 LIMIT 1";

    private static final String COUNT_BY_REPOSITORY =
            "SELECT COUNT(*) FROM branches WHERE repository_id = ?";

    private static final String EXISTS_BY_NAME =
            "SELECT 1 FROM branches WHERE repository_id = ? AND name = ? COLLATE NOCASE "
                    + "AND (? IS NULL OR id <> ?) LIMIT 1";

    private static final String DEACTIVATE_BY_REPOSITORY =
            "UPDATE branches SET is_active = 0, status = ? WHERE repository_id = ?";

    private static final String CLEAR_PARENT_REFERENCES =
            "UPDATE branches SET parent_branch_id = ? WHERE parent_branch_id = ?";

    public long create(Branch branch) throws SQLException {
        if (branch.getCreatedAt() == null) {
            branch.setCreatedAt(Instant.now());
        }
        if (branch.getStatus() == null || branch.getStatus().isBlank()) {
            branch.setStatus(branch.isActive() ? Branch.STATUS_ACTIVE : Branch.STATUS_INACTIVE);
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            statement.setLong(1, branch.getRepositoryId());
            statement.setString(2, branch.getName());
            statement.setInt(3, branch.isActive() ? 1 : 0);
            statement.setString(4, InstantFormats.format(branch.getCreatedAt()));
            bindOptionalLong(statement, 5, branch.getParentBranchId());
            statement.setString(6, branch.getDescription());
            statement.setString(7, branch.getLatestCommitHash());
            statement.setString(8, branch.getStatus());
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
            bindOptionalLong(statement, 4, branch.getParentBranchId());
            statement.setString(5, branch.getDescription());
            statement.setString(6, branch.getLatestCommitHash());
            statement.setString(7, branch.getStatus());
            statement.setLong(8, branch.getId());
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
        String pattern = likePattern(query);
        String exact = query == null ? "" : query.trim();
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, exact);
            return queryList(statement, this::mapRow);
        }
    }

    public List<Branch> searchByRepository(long repositoryId, String query) throws SQLException {
        String pattern = likePattern(query);
        String exact = query == null ? "" : query.trim();
        try (PreparedStatement statement = connection().prepareStatement(SEARCH_BY_REPOSITORY)) {
            statement.setLong(1, repositoryId);
            statement.setString(2, pattern);
            statement.setString(3, exact);
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

    public boolean existsByNameIgnoreCase(long repositoryId, String name, Long excludeId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(EXISTS_BY_NAME)) {
            statement.setLong(1, repositoryId);
            statement.setString(2, name);
            if (excludeId == null) {
                statement.setNull(3, Types.INTEGER);
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setLong(3, excludeId);
                statement.setLong(4, excludeId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void deactivateAllInRepository(long repositoryId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DEACTIVATE_BY_REPOSITORY)) {
            statement.setString(1, Branch.STATUS_INACTIVE);
            statement.setLong(2, repositoryId);
            statement.executeUpdate();
        }
    }

    public void reparentChildren(long oldParentId, Long newParentId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(CLEAR_PARENT_REFERENCES)) {
            if (newParentId == null) {
                statement.setNull(1, Types.INTEGER);
            } else {
                statement.setLong(1, newParentId);
            }
            statement.setLong(2, oldParentId);
            statement.executeUpdate();
        }
    }

    private static void bindOptionalLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private Branch mapRow(ResultSet resultSet) throws SQLException {
        Branch branch = new Branch(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                resultSet.getString("name"),
                resultSet.getInt("is_active") == 1,
                InstantFormats.parse(resultSet.getString("created_at"))
        );
        long parentId = resultSet.getLong("parent_branch_id");
        branch.setParentBranchId(resultSet.wasNull() ? null : parentId);
        branch.setDescription(resultSet.getString("description"));
        branch.setLatestCommitHash(resultSet.getString("latest_commit"));
        String status = resultSet.getString("status");
        if (status != null && !status.isBlank()) {
            branch.setStatus(status);
        }
        return branch;
    }
}
