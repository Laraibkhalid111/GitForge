package com.gitforge.dao;

import com.gitforge.database.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared JDBC helpers for DAO implementations.
 */
abstract class AbstractDAO {

    protected Connection connection() throws SQLException {
        return ConnectionManager.getInstance().getConnection();
    }

    protected long executeInsert(PreparedStatement statement) throws SQLException {
        int affected = statement.executeUpdate();
        if (affected == 0) {
            throw new SQLException("Insert failed; no rows affected");
        }
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Insert failed; no generated key returned");
    }

    protected <T> Optional<T> queryOne(PreparedStatement statement, RowMapper<T> mapper) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Optional.of(mapper.map(resultSet));
            }
            return Optional.empty();
        }
    }

    protected <T> List<T> queryList(PreparedStatement statement, RowMapper<T> mapper) throws SQLException {
        List<T> results = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                results.add(mapper.map(resultSet));
            }
        }
        return results;
    }

    protected boolean executeUpdate(PreparedStatement statement) throws SQLException {
        return statement.executeUpdate() > 0;
    }

    protected String likePattern(String query) {
        String trimmed = query == null ? "" : query.trim();
        return "%" + trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    protected PreparedStatement prepareInsert(Connection connection, String sql) throws SQLException {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    @FunctionalInterface
    protected interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
