package com.gitforge.service;

import com.gitforge.database.ConnectionManager;
import com.gitforge.model.Branch;
import com.gitforge.model.Commit;
import com.gitforge.model.CommitSummary;
import com.gitforge.model.Repository;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.util.CommitHashGenerator;
import com.gitforge.util.LinkedList;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Application service for simulated commit management.
 * Commit history is held in a custom {@link LinkedList} while SQLite remains persistent storage.
 */
public class CommitService {

    public static final List<String> COMMIT_TYPES = List.of(
            "Feature", "Bug Fix", "Refactor", "Documentation", "Test", "Style", "Chore"
    );

    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;
    private final RepositoryRepository repositoryRepository;
    private final LinkedList<Commit> commitHistory = new LinkedList<>();

    public CommitService() {
        this(new CommitRepository(), new BranchRepository(), new RepositoryRepository());
    }

    public CommitService(CommitRepository commitRepository,
                         BranchRepository branchRepository,
                         RepositoryRepository repositoryRepository) {
        this.commitRepository = commitRepository;
        this.branchRepository = branchRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Returns commit summaries by traversing the in-memory linked list.
     */
    public List<CommitSummary> displayCommitHistory() throws SQLException {
        return mapHistoryToSummaries(commitHistory.toList());
    }

    public List<CommitSummary> listSummaries(Long repositoryId) throws SQLException {
        reloadHistory(repositoryId, null);
        return displayCommitHistory();
    }

    public List<CommitSummary> searchSummaries(Long repositoryId, String query) throws SQLException {
        reloadHistory(repositoryId, query);
        return displayCommitHistory();
    }

    public Optional<CommitSummary> findSummaryById(long id) throws SQLException {
        Optional<Commit> commit = findCommit(id);
        if (commit.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toSummary(commit.get()));
    }

    public Optional<Commit> findCommit(long id) {
        return commitHistory.find(candidate -> candidate.getId() != null && candidate.getId() == id);
    }

    public List<Commit> traverseHistory() {
        return commitHistory.toList();
    }

    public LinkedList<Commit> getCommitHistory() {
        return commitHistory;
    }

    public List<Branch> listBranchesForRepository(long repositoryId) throws SQLException {
        return branchRepository.findByRepositoryId(repositoryId);
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public CommitSummary createCommit(long repositoryId, long branchId, String message,
                                      String author, String commitType) throws SQLException {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Commit message is required");
        }
        repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        if (branch.getRepositoryId() == null || branch.getRepositoryId() != repositoryId) {
            throw new IllegalArgumentException("Branch does not belong to the selected repository");
        }

        String normalizedType = normalizeCommitType(commitType);
        String normalizedAuthor = normalizeAuthor(author);
        Instant committedAt = Instant.now();
        String parentHash = commitRepository.findLatestByBranchId(branchId)
                .map(Commit::getHash)
                .orElse(null);
        String hash = generateUniqueHash(repositoryId, branchId, message, normalizedAuthor, committedAt);
        int filesChanged = simulateFilesChanged(normalizedType);

        Commit commit = new Commit();
        commit.setRepositoryId(repositoryId);
        commit.setBranchId(branchId);
        commit.setHash(hash);
        commit.setMessage(message.trim());
        commit.setAuthor(normalizedAuthor);
        commit.setCommittedAt(committedAt);
        commit.setParentHash(parentHash);
        commit.setCommitType(normalizedType);
        commit.setFilesChanged(filesChanged);

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            commitRepository.create(commit);
            connection.commit();
            Commit persisted = commitRepository.findById(commit.getId()).orElseThrow();
            insertCommit(persisted);
            return toSummary(persisted);
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to create commit", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public boolean deleteCommit(long id) throws SQLException {
        if (commitRepository.delete(id)) {
            deleteCommitFromHistory(id);
            return true;
        }
        return false;
    }

    public void insertCommit(Commit commit) {
        commitHistory.insertFirst(commit);
    }

    public boolean deleteCommitFromHistory(long id) {
        return commitHistory.deleteIf(candidate -> candidate.getId() != null && candidate.getId() == id);
    }

    private void reloadHistory(Long repositoryId, String query) throws SQLException {
        commitHistory.clear();
        List<Commit> commits;
        if (query == null || query.isBlank()) {
            commits = repositoryId == null
                    ? commitRepository.findAll()
                    : commitRepository.findByRepositoryId(repositoryId);
        } else {
            commits = repositoryId == null
                    ? commitRepository.search(query.trim())
                    : commitRepository.searchByRepository(repositoryId, query.trim());
        }
        for (Commit commit : commits) {
            commitHistory.insertLast(commit);
        }
    }

    private List<CommitSummary> mapHistoryToSummaries(List<Commit> commits) throws SQLException {
        if (commits.isEmpty()) {
            return List.of();
        }
        Map<Long, String> branchNames = branchRepository.findAll().stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
        Map<Long, String> repositoryNames = repositoryRepository.findAll().stream()
                .collect(Collectors.toMap(Repository::getId, Repository::getName, (a, b) -> a));

        List<CommitSummary> summaries = new ArrayList<>(commits.size());
        for (Commit commit : commits) {
            String branchName = commit.getBranchId() == null
                    ? "—"
                    : branchNames.getOrDefault(commit.getBranchId(), "—");
            String repositoryName = repositoryNames.getOrDefault(commit.getRepositoryId(), "—");
            summaries.add(new CommitSummary(commit, branchName, repositoryName));
        }
        return summaries;
    }

    private CommitSummary toSummary(Commit commit) throws SQLException {
        String branchName = "—";
        if (commit.getBranchId() != null) {
            branchName = branchRepository.findById(commit.getBranchId())
                    .map(Branch::getName)
                    .orElse("—");
        }
        String repositoryName = repositoryRepository.findById(commit.getRepositoryId())
                .map(Repository::getName)
                .orElse("—");
        return new CommitSummary(commit, branchName, repositoryName);
    }

    private String generateUniqueHash(long repositoryId, long branchId, String message,
                                      String author, Instant committedAt) throws SQLException {
        for (int attempt = 0; attempt < 5; attempt++) {
            String hash = CommitHashGenerator.generate(message, author, committedAt, repositoryId, branchId);
            if (!commitRepository.existsByHash(repositoryId, hash)) {
                return hash;
            }
        }
        throw new SQLException("Unable to generate a unique commit hash");
    }

    private static String normalizeCommitType(String commitType) {
        if (commitType == null || commitType.isBlank()) {
            return "Feature";
        }
        for (String type : COMMIT_TYPES) {
            if (type.equalsIgnoreCase(commitType.trim())) {
                return type;
            }
        }
        return commitType.trim();
    }

    private static String normalizeAuthor(String author) {
        if (author == null || author.isBlank()) {
            return "GitForge User";
        }
        return author.trim();
    }

    static int simulateFilesChanged(String commitType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (commitType) {
            case "Feature" -> random.nextInt(2, 9);
            case "Bug Fix" -> random.nextInt(1, 4);
            case "Refactor" -> random.nextInt(3, 12);
            case "Documentation" -> random.nextInt(1, 6);
            case "Test" -> random.nextInt(1, 8);
            case "Style" -> random.nextInt(1, 5);
            case "Chore" -> random.nextInt(1, 4);
            default -> random.nextInt(1, 6);
        };
    }
}
