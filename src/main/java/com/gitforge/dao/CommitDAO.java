package com.gitforge.dao;

import com.gitforge.model.Commit;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the commits table.
 */
public class CommitDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO commits (repository_id, branch_id, hash, message, author, committed_at, parent_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, repository_id, branch_id, hash, message, author, committed_at, parent_hash
            FROM commits
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE commits
            SET repository_id = ?, branch_id = ?, hash = ?, message = ?, author = ?,
                committed_at = ?, parent_hash = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM commits WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, repository_id, branch_id, hash, message, author, committed_at, parent_hash
            FROM commits
            ORDER BY committed_at DESC
            """;

    private static final String SEARCH = """
            SELECT id, repository_id, branch_id, hash, message, author, committed_at, parent_hash
            FROM commits
            WHERE hash LIKE ? ESCAPE '\\'
               OR IFNULL(message, '') LIKE ? ESCAPE '\\'
               OR IFNULL(author, '') LIKE ? ESCAPE '\\'
            ORDER BY committed_at DESC
            """;

    private static final String COUNT_BY_REPOSITORY = """
            SELECT COUNT(*)
            FROM commits
            WHERE repository_id = ?
            """;

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM commits";

    public long create(Commit commit) throws SQLException {
        if (commit.getCommittedAt() == null) {
            commit.setCommittedAt(Instant.now());
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            bindWritable(statement, commit);
            long id = executeInsert(statement);
            commit.setId(id);
            return id;
        }
    }

    public Optional<Commit> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Commit commit) throws SQLException {
        if (commit.getId() == null) {
            throw new IllegalArgumentException("Commit id is required for update");
        }
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            bindWritable(statement, commit);
            statement.setLong(8, commit.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return executeUpdate(statement);
        }
    }

    public List<Commit> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Commit> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            return queryList(statement, this::mapRow);
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

    public int countAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(COUNT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void bindWritable(PreparedStatement statement, Commit commit) throws SQLException {
        statement.setLong(1, commit.getRepositoryId());
        if (commit.getBranchId() == null) {
            statement.setNull(2, Types.INTEGER);
        } else {
            statement.setLong(2, commit.getBranchId());
        }
        statement.setString(3, commit.getHash());
        statement.setString(4, commit.getMessage());
        statement.setString(5, commit.getAuthor());
        statement.setString(6, InstantFormats.format(commit.getCommittedAt()));
        statement.setString(7, commit.getParentHash());
    }

    private Commit mapRow(ResultSet resultSet) throws SQLException {
        long branchId = resultSet.getLong("branch_id");
        Long mappedBranchId = resultSet.wasNull() ? null : branchId;
        return new Commit(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                mappedBranchId,
                resultSet.getString("hash"),
                resultSet.getString("message"),
                resultSet.getString("author"),
                InstantFormats.parse(resultSet.getString("committed_at")),
                resultSet.getString("parent_hash")
        );
    }
}
