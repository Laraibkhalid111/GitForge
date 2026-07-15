package com.gitforge.repository;

import com.gitforge.dao.MergeDAO;
import com.gitforge.model.Merge;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link MergeDAO}.
 */
public class MergeRepository {

    private final MergeDAO dao;

    public MergeRepository() {
        this(new MergeDAO());
    }

    public MergeRepository(MergeDAO dao) {
        this.dao = dao;
    }

    public long create(Merge merge) throws SQLException {
        return dao.create(merge);
    }

    public Optional<Merge> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public boolean update(Merge merge) throws SQLException {
        return dao.update(merge);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Merge> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Merge> findByRepositoryId(long repositoryId) throws SQLException {
        return dao.findByRepositoryId(repositoryId);
    }

    public List<Merge> search(String query) throws SQLException {
        return dao.search(query);
    }

    public int countAll() throws SQLException {
        return dao.countAll();
    }

    public int countByRepositoryId(long repositoryId) throws SQLException {
        return dao.countByRepositoryId(repositoryId);
    }
}
