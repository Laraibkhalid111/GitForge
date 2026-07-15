package com.gitforge.repository;

import com.gitforge.dao.BranchDAO;
import com.gitforge.model.Branch;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link BranchDAO}.
 */
public class BranchRepository {

    private final BranchDAO dao;

    public BranchRepository() {
        this(new BranchDAO());
    }

    public BranchRepository(BranchDAO dao) {
        this.dao = dao;
    }

    public long create(Branch branch) throws SQLException {
        return dao.create(branch);
    }

    public Optional<Branch> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public boolean update(Branch branch) throws SQLException {
        return dao.update(branch);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Branch> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Branch> search(String query) throws SQLException {
        return dao.search(query);
    }

    public List<Branch> searchByRepository(long repositoryId, String query) throws SQLException {
        return dao.searchByRepository(repositoryId, query);
    }

    public List<Branch> findByRepositoryId(long repositoryId) throws SQLException {
        return dao.findByRepositoryId(repositoryId);
    }

    public Optional<Branch> findActiveByRepositoryId(long repositoryId) throws SQLException {
        return dao.findActiveByRepositoryId(repositoryId);
    }

    public int countByRepositoryId(long repositoryId) throws SQLException {
        return dao.countByRepositoryId(repositoryId);
    }

    public boolean existsByNameIgnoreCase(long repositoryId, String name, Long excludeId) throws SQLException {
        return dao.existsByNameIgnoreCase(repositoryId, name, excludeId);
    }

    public void deactivateAllInRepository(long repositoryId) throws SQLException {
        dao.deactivateAllInRepository(repositoryId);
    }

    public void reparentChildren(long oldParentId, Long newParentId) throws SQLException {
        dao.reparentChildren(oldParentId, newParentId);
    }
}
