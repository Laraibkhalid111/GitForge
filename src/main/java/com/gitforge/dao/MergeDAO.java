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

    private static final String COLUMNS =
            "id, repository_id, source_branch_id, target_branch_id, status, message, merged_at, "
                    + "strategy, merge_commit, conflict_status";

    private static final String INSERT =
            "INSERT INTO merges (repository_id, source_branch_id, target_branch_id, status, message, "
                    + "merged_at, strategy, merge_commit, conflict_status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM merges WHERE id = ?";

    private static final String UPDATE =
            "UPDATE merges SET repository_id = ?, source_branch_id = ?, target_branch_id = ?, "
                    + "status = ?, message = ?, merged_at = ?, strategy = ?, merge_commit = ?, "
                    + "conflict_status = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM merges WHERE id = ?";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM merges ORDER BY merged_at DESC";

    private static final String SELECT_BY_REPOSITORY =
            "SELECT " + COLUMNS + " FROM merges WHERE repository_id = ? ORDER BY merged_at DESC";

    private static final String SEARCH =
            "SELECT " + COLUMNS + " FROM merges WHERE "
                    + "status LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(message, '') LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(strategy, '') LIKE ? ESCAPE '\\' "
                    + "OR IFNULL(conflict_status, '') LIKE ? ESCAPE '\\' "
                    + "ORDER BY merged_at DESC";

    private static final String COUNT_ALL = "SELECT COUNT(*) FROM merges";

    private static final String COUNT_BY_REPOSITORY =
            "SELECT COUNT(*) FROM merges WHERE repository_id = ?";

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
            statement.setLong(10, merge.getId());
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

    public List<Merge> findByRepositoryId(long repositoryId) throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(SELECT_BY_REPOSITORY)) {
            statement.setLong(1, repositoryId);
            return queryList(statement, this::mapRow);
        }
    }

    public List<Merge> search(String query) throws SQLException {
        String pattern = likePattern(query);
        try (PreparedStatement statement = connection().prepareStatement(SEARCH)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            return queryList(statement, this::mapRow);
        }
    }

    public int countAll() throws SQLException {
        try (PreparedStatement statement = connection().prepareStatement(COUNT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
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

    private void bindWritable(PreparedStatement statement, Merge merge) throws SQLException {
        statement.setLong(1, merge.getRepositoryId());
        setNullableLong(statement, 2, merge.getSourceBranchId());
        setNullableLong(statement, 3, merge.getTargetBranchId());
        statement.setString(4, merge.getStatus());
        statement.setString(5, merge.getMessage());
        statement.setString(6, InstantFormats.format(merge.getMergedAt()));
        statement.setString(7, merge.getStrategy());
        statement.setString(8, merge.getMergeCommitHash());
        statement.setString(9, merge.getConflictStatus());
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private Merge mapRow(ResultSet resultSet) throws SQLException {
        Merge merge = new Merge(
                resultSet.getLong("id"),
                resultSet.getLong("repository_id"),
                readNullableLong(resultSet, "source_branch_id"),
                readNullableLong(resultSet, "target_branch_id"),
                resultSet.getString("status"),
                resultSet.getString("message"),
                InstantFormats.parse(resultSet.getString("merged_at"))
        );
        merge.setStrategy(resultSet.getString("strategy"));
        merge.setMergeCommitHash(resultSet.getString("merge_commit"));
        merge.setConflictStatus(resultSet.getString("conflict_status"));
        return merge;
    }

    private static Long readNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }
}
