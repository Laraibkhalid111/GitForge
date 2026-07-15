package com.gitforge.repository;

import com.gitforge.dao.CommitDAO;
import com.gitforge.model.Commit;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link CommitDAO}.
 */
public class CommitRepository {

    private final CommitDAO dao;

    public CommitRepository() {
        this(new CommitDAO());
    }

    public CommitRepository(CommitDAO dao) {
        this.dao = dao;
    }

    public long create(Commit commit) throws SQLException {
        return dao.create(commit);
    }

    public Optional<Commit> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public Optional<Commit> findLatestByBranchId(long branchId) throws SQLException {
        return dao.findLatestByBranchId(branchId);
    }

    public boolean existsByHash(long repositoryId, String hash) throws SQLException {
        return dao.existsByHash(repositoryId, hash);
    }

    public boolean update(Commit commit) throws SQLException {
        return dao.update(commit);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Commit> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Commit> findByRepositoryId(long repositoryId) throws SQLException {
        return dao.findByRepositoryId(repositoryId);
    }

    public List<Commit> search(String query) throws SQLException {
        return dao.search(query);
    }

    public List<Commit> searchByRepository(long repositoryId, String query) throws SQLException {
        return dao.searchByRepository(repositoryId, query);
    }

    public int countByRepositoryId(long repositoryId) throws SQLException {
        return dao.countByRepositoryId(repositoryId);
    }

    public int countByBranchId(long branchId) throws SQLException {
        return dao.countByBranchId(branchId);
    }

    public int countAll() throws SQLException {
        return dao.countAll();
    }
}
