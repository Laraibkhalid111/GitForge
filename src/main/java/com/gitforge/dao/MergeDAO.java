package com.gitforge.dao;

import com.gitforge.model.Merge;
import com.gitforge.util.InstantFormats;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the merges table.
 */
public class MergeDAO extends AbstractDAO {

    private static final String INSERT = """
            INSERT INTO merges (repository_id, source_branch_id, target_branch_id, status, message, merged_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, repository_id, source_branch_id, target_branch_id, status, message, merged_at
            FROM merges
            WHERE id = ?
            """;

    private static final String UPDATE = """
            UPDATE merges
            SET repository_id = ?, source_branch_id = ?, target_branch_id = ?,
                status = ?, message = ?, merged_at = ?
            WHERE id = ?
            """;

    private static final String DELETE = "DELETE FROM merges WHERE id = ?";

    private static final String SELECT_ALL = """
            SELECT id, repository_id, source_branch_id, target_branch_id, status, message, merged_at
            FROM merges
            ORDER BY merged_at DESC
            """;

    private static final String SEARCH = """
            SELECT id, repository_id, source_branch_id, target_branch_id, status, message, merged_at
            FROM merges
            WHERE status LIKE ? ESCAPE '\\'
               OR IFNULL(message, '') LIKE ? ESCAPE '\\'
            ORDER BY merged_at DESC
            """;

    public long create(Merge merge) throws SQLException {
        if (merge.getMergedAt() == null) {
            merge.setMergedAt(Instant.now());
        }
        try (PreparedStatement statement = prepareInsert(connection(), INSERT)) {
            bindWritable(statement, merge);
            long id = executeInsert(statement);
            merge.setId(id);
            return id;
        }
    }

    public Optional<Merge> findById(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_ID)) {
            statement.setLong(1, id);
            return queryOne(statement, this::mapRow);
        }
    }

    public boolean update(Merge merge) throws SQLException {
        if (merge.getId() == null) {
            throw new IllegalArgumentException("Merge id is required for update");
        }
        try (PreparedStatement statement = connection().prepareStatement(UPDATE)) {
            bindWritable(statement, merge);
            statement.setLong(7, merge.getId());
            return executeUpdate(statement);
        }
    }

    public boolean delete(long id) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(DELETE)) {
            statement.setLong(1, id);
            return executeUpdate(statement);
        }
    }

    public List<Merge> findAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_ALL)) {
            return queryList(statement, this::mapRow);
        }
    }

    public List<Merge> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            return queryList(statement, this::mapRow);
        }
    }

    private void bindWritable(PreparedStatement statement, Merge merge) throws SQLException {
        statement.setLong(1, merge.getRepositoryId());
        setNullableLong(statement, 2, merge.getSourceBranchId());
        setNullableLong(statement, 3, merge.getTargetBranchId());
        statement.setString(4, merge.getStatus());
        statement.setString(5, merge.getMessage());
        statement.setString(6, InstantFormats.format(merge.getMergedAt()));
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private Merge mapRow(ResultSet resultSet) throws SQLException {
        return new Merge(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                readNullableLong(resultSet, "source_branch_id"),
                readNullableLong(resultSet, "target_branch_id"),
                resultSet.getString("status"),
                resultSet.getString("message"),
                InstantFormats.parse(resultSet.getString("merged_at"))
        );
    }

    private static Long readNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
