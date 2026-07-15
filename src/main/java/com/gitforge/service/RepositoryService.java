package com.gitforge.service;

import com.gitforge.database.ConnectionManager;
import com.gitforge.model.Branch;
import com.gitforge.model.Repository;
import com.gitforge.model.RepositorySummary;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.RepositoryRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service for simulated repository management.
 * Persists through existing DAOs/repositories — no Git CLI usage.
 */
public class RepositoryService {

    public static final String DEFAULT_BRANCH = "main";

    private final RepositoryRepository repositoryRepository;
    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;

    public RepositoryService() {
        this(new RepositoryRepository(), new BranchRepository(), new CommitRepository());
    }

    public RepositoryService(RepositoryRepository repositoryRepository,
                             BranchRepository branchRepository,
                             CommitRepository commitRepository) {
        this.repositoryRepository = repositoryRepository;
        this.branchRepository = branchRepository;
        this.commitRepository = commitRepository;
    }

    public List<RepositorySummary> listSummaries() throws SQLException {
        return toSummaries(repositoryRepository.findAll());
    }

    public List<RepositorySummary> searchSummaries(String query) throws SQLException {
        if (query == null || query.isBlank()) {
            return listSummaries();
        }
        return toSummaries(repositoryRepository.search(query.trim()));
    }

    public Optional<RepositorySummary> findSummaryById(long id) throws SQLException {
        Optional<Repository> repository = repositoryRepository.findById(id);
        if (repository.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toSummary(repository.get()));
    }

    public RepositorySummary createRepository(String name, String description, String path, String defaultBranch)
            throws SQLException {
        String normalizedName = requireUniqueName(name, null);
        String branchName = normalizeBranchName(defaultBranch);
        String resolvedPath = resolvePath(path, normalizedName);

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            Repository repository = new Repository(normalizedName, resolvedPath, trimToNull(description));
            long repositoryId = repositoryRepository.create(repository);

            Branch branch = new Branch(repositoryId, branchName, true);
            branch.setStatus(Branch.STATUS_ACTIVE);
            branchRepository.create(branch);

            connection.commit();
            return toSummary(repositoryRepository.findById(repositoryId).orElseThrow());
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to create repository", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public RepositorySummary updateRepository(long id, String name, String description, String path)
            throws SQLException {
        Repository existing = repositoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        String normalizedName = requireUniqueName(name, id);
        existing.setName(normalizedName);
        existing.setDescription(trimToNull(description));
        existing.setPath(resolvePath(path, normalizedName));

        if (!repositoryRepository.update(existing)) {
            throw new SQLException("Repository update failed");
        }
        return toSummary(repositoryRepository.findById(id).orElseThrow());
    }

    public boolean deleteRepository(long id) throws SQLException {
        return repositoryRepository.delete(id);
    }

    public int countRepositories() throws SQLException {
        return repositoryRepository.countAll();
    }

    public int countAllCommits() throws SQLException {
        return commitRepository.countAll();
    }

    private List<RepositorySummary> toSummaries(List<Repository> repositories) throws SQLException {
        List<RepositorySummary> summaries = new ArrayList<>(repositories.size());
        for (Repository repository : repositories) {
            summaries.add(toSummary(repository));
        }
        return summaries;
    }

    private RepositorySummary toSummary(Repository repository) throws SQLException {
        long repositoryId = repository.getId();
        String currentBranch = branchRepository.findActiveByRepositoryId(repositoryId)
                .map(Branch::getName)
                .orElse("—");
        int totalBranches = branchRepository.countByRepositoryId(repositoryId);
        int totalCommits = commitRepository.countByRepositoryId(repositoryId);
        return new RepositorySummary(
                repository,
                currentBranch,
                totalBranches,
                totalCommits,
                RepositorySummary.STATUS_ACTIVE
        );
    }

    private String requireUniqueName(String name, Long excludeId) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Repository name is required");
        }
        String normalized = name.trim();
        if (repositoryRepository.existsByNameIgnoreCase(normalized, excludeId)) {
            throw new IllegalArgumentException("A repository named \"" + normalized + "\" already exists");
        }
        return normalized;
    }

    private static String normalizeBranchName(String defaultBranch) {
        if (defaultBranch == null || defaultBranch.isBlank()) {
            return DEFAULT_BRANCH;
        }
        return defaultBranch.trim();
    }

    private static String resolvePath(String path, String repositoryName) {
        if (path != null && !path.isBlank()) {
            return path.trim();
        }
        return Path.of(System.getProperty("user.home"), "gitforge-repos", repositoryName)
                .toString();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
