package com.gitforge.repository;

import com.gitforge.dao.StashDAO;
import com.gitforge.model.Stash;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link StashDAO}.
 */
public class StashRepository {

    private final StashDAO dao;

    public StashRepository() {
        this(new StashDAO());
    }

    public StashRepository(StashDAO dao) {
        this.dao = dao;
    }

    public long create(Stash stash) throws SQLException {
        return dao.create(stash);
    }

    public Optional<Stash> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public boolean update(Stash stash) throws SQLException {
        return dao.update(stash);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Stash> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Stash> search(String query) throws SQLException {
        return dao.search(query);
    }
}
