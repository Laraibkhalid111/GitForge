package com.gitforge.repository;

import com.gitforge.dao.RepositoryDAO;
import com.gitforge.model.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link RepositoryDAO}.
 * Provides persistence access only — no Git or UI logic.
 */
public class RepositoryRepository {

    private final RepositoryDAO dao;

    public RepositoryRepository() {
        this(new RepositoryDAO());
    }

    public RepositoryRepository(RepositoryDAO dao) {
        this.dao = dao;
    }

    public long create(Repository repository) throws SQLException {
        return dao.create(repository);
    }

    public Optional<Repository> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public boolean update(Repository repository) throws SQLException {
        return dao.update(repository);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Repository> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Repository> search(String query) throws SQLException {
        return dao.search(query);
    }

    public Optional<Repository> findByNameIgnoreCase(String name) throws SQLException {
        return dao.findByNameIgnoreCase(name);
    }

    public boolean existsByNameIgnoreCase(String name, Long excludeId) throws SQLException {
        return dao.existsByNameIgnoreCase(name, excludeId);
    }

    public int countAll() throws SQLException {
        return dao.countAll();
    }
}
