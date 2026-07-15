package com.gitforge.service;

import com.gitforge.database.ConnectionManager;
import com.gitforge.model.Branch;
import com.gitforge.model.Commit;
import com.gitforge.model.Merge;
import com.gitforge.model.MergePreview;
import com.gitforge.model.MergeSummary;
import com.gitforge.model.Repository;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.MergeRepository;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.util.CommitHashGenerator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Application service for simulated merge management.
 * No Git CLI is used — merges are persisted to SQLite and update related metadata.
 */
public class MergeService {

    public static final List<String> STRATEGIES = List.of(
            Merge.STRATEGY_FAST_FORWARD,
            Merge.STRATEGY_THREE_WAY,
            Merge.STRATEGY_SIMULATION_ONLY
    );

    private final MergeRepository mergeRepository;
    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;
    private final RepositoryRepository repositoryRepository;

    public MergeService() {
        this(new MergeRepository(), new BranchRepository(), new CommitRepository(), new RepositoryRepository());
    }

    public MergeService(MergeRepository mergeRepository,
                        BranchRepository branchRepository,
                        CommitRepository commitRepository,
                        RepositoryRepository repositoryRepository) {
        this.mergeRepository = mergeRepository;
        this.branchRepository = branchRepository;
        this.commitRepository = commitRepository;
        this.repositoryRepository = repositoryRepository;
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public List<Branch> listBranchesForRepository(long repositoryId) throws SQLException {
        return branchRepository.findByRepositoryId(repositoryId);
    }

    public List<MergeSummary> listHistory(Long repositoryId) throws SQLException {
        List<Merge> merges = repositoryId == null
                ? mergeRepository.findAll()
                : mergeRepository.findByRepositoryId(repositoryId);
        return toSummaries(merges);
    }

    public int countAllMerges() throws SQLException {
        return mergeRepository.countAll();
    }

    public MergePreview previewMerge(long repositoryId, long sourceBranchId, long targetBranchId, String strategy)
            throws SQLException {
        Branch source = requireBranch(repositoryId, sourceBranchId, "Source");
        Branch target = requireBranch(repositoryId, targetBranchId, "Target");
        if (Objects.equals(source.getId(), target.getId())) {
            throw new IllegalArgumentException("Source and target branches must be different");
        }

        String normalizedStrategy = normalizeStrategy(strategy);
        String sourceLatest = resolveLatestHash(source);
        String targetLatest = resolveLatestHash(target);
        int commitsToMerge = estimateCommitsToMerge(source, target, sourceLatest, targetLatest);
        String conflictStatus = simulateConflictStatus();
        String expectedHash = CommitHashGenerator.generate(
                "Merge " + source.getName() + " into " + target.getName(),
                "GitForge Merge",
                Instant.now(),
                repositoryId,
                targetBranchId
        );

        return new MergePreview(
                source.getName(),
                target.getName(),
                sourceLatest,
                targetLatest,
                commitsToMerge,
                expectedHash,
                conflictStatus,
                normalizedStrategy
        );
    }

    /**
     * Executes a simulated merge. Creates a merge record, optional merge commit,
     * updates the target branch tip, and refreshes repository statistics.
     *
     * @return the persisted merge summary
     */
    public MergeSummary executeMerge(long repositoryId, long sourceBranchId, long targetBranchId,
                                     String strategy, String conflictStatus, boolean userContinued)
            throws SQLException {
        if (!userContinued) {
            throw new IllegalArgumentException("Merge was cancelled by the user");
        }

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));
        Branch source = requireBranch(repositoryId, sourceBranchId, "Source");
        Branch target = requireBranch(repositoryId, targetBranchId, "Target");
        if (Objects.equals(source.getId(), target.getId())) {
            throw new IllegalArgumentException("Source and target branches must be different");
        }

        String normalizedStrategy = normalizeStrategy(strategy);
        String normalizedConflict = normalizeConflict(conflictStatus);
        Instant mergedAt = Instant.now();
        String parentHash = resolveLatestHash(target);
        String mergeMessage = "Merge branch '" + source.getName() + "' into " + target.getName();

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            String mergeCommitHash = null;
            String status;

            if (Merge.STRATEGY_SIMULATION_ONLY.equals(normalizedStrategy)) {
                status = Merge.STATUS_SIMULATED;
                mergeCommitHash = CommitHashGenerator.generate(
                        mergeMessage, "GitForge Merge", mergedAt, repositoryId, targetBranchId);
            } else {
                status = Merge.STATUS_COMPLETED;
                mergeCommitHash = generateUniqueHash(
                        repositoryId, targetBranchId, mergeMessage, "GitForge Merge", mergedAt);

                Commit mergeCommit = new Commit();
                mergeCommit.setRepositoryId(repositoryId);
                mergeCommit.setBranchId(targetBranchId);
                mergeCommit.setHash(mergeCommitHash);
                mergeCommit.setMessage(mergeMessage);
                mergeCommit.setAuthor("GitForge Merge");
                mergeCommit.setCommittedAt(mergedAt);
                mergeCommit.setParentHash(parentHash);
                mergeCommit.setCommitType("Merge");
                mergeCommit.setFilesChanged(simulateFilesChanged(normalizedConflict));
                commitRepository.create(mergeCommit);

                target.setLatestCommitHash(mergeCommitHash);
                branchRepository.update(target);
            }

            Merge merge = new Merge(repositoryId, sourceBranchId, targetBranchId, status, mergeMessage, mergedAt);
            merge.setStrategy(normalizedStrategy);
            merge.setMergeCommitHash(mergeCommitHash);
            merge.setConflictStatus(normalizedConflict);
            mergeRepository.create(merge);

            repository.setUpdatedAt(mergedAt);
            repositoryRepository.update(repository);

            connection.commit();
            return toSummary(mergeRepository.findById(merge.getId()).orElseThrow());
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to execute merge", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public static String simulateConflictStatus() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 55) {
            return Merge.CONFLICT_NONE;
        }
        if (roll < 85) {
            return Merge.CONFLICT_MINOR;
        }
        return Merge.CONFLICT_MAJOR;
    }

    private Branch requireBranch(long repositoryId, long branchId, String label) throws SQLException {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException(label + " branch not found"));
        if (branch.getRepositoryId() == null || branch.getRepositoryId() != repositoryId) {
            throw new IllegalArgumentException(label + " branch does not belong to the selected repository");
        }
        return branch;
    }

    private String resolveLatestHash(Branch branch) throws SQLException {
        if (branch.getLatestCommitHash() != null && !branch.getLatestCommitHash().isBlank()) {
            return branch.getLatestCommitHash();
        }
        return commitRepository.findLatestByBranchId(branch.getId())
                .map(Commit::getHash)
                .orElse(null);
    }

    private int estimateCommitsToMerge(Branch source, Branch target, String sourceLatest, String targetLatest)
            throws SQLException {
        if (Objects.equals(sourceLatest, targetLatest) && sourceLatest != null) {
            return 0;
        }
        int sourceCount = commitRepository.countByBranchId(source.getId());
        int targetCount = commitRepository.countByBranchId(target.getId());
        int delta = Math.max(0, sourceCount - targetCount);
        if (delta == 0 && sourceLatest != null && !Objects.equals(sourceLatest, targetLatest)) {
            return 1;
        }
        return delta;
    }

    private List<MergeSummary> toSummaries(List<Merge> merges) throws SQLException {
        List<MergeSummary> summaries = new ArrayList<>(merges.size());
        for (Merge merge : merges) {
            summaries.add(toSummary(merge));
        }
        return summaries;
    }

    private MergeSummary toSummary(Merge merge) throws SQLException {
        String sourceName = merge.getSourceBranchId() == null
                ? "—"
                : branchRepository.findById(merge.getSourceBranchId()).map(Branch::getName).orElse("—");
        String targetName = merge.getTargetBranchId() == null
                ? "—"
                : branchRepository.findById(merge.getTargetBranchId()).map(Branch::getName).orElse("—");
        String repositoryName = repositoryRepository.findById(merge.getRepositoryId())
                .map(Repository::getName)
                .orElse("—");
        return new MergeSummary(merge, sourceName, targetName, repositoryName);
    }

    private String generateUniqueHash(long repositoryId, long branchId, String message,
                                      String author, Instant committedAt) throws SQLException {
        for (int attempt = 0; attempt < 5; attempt++) {
            String hash = CommitHashGenerator.generate(message, author, committedAt, repositoryId, branchId);
            if (!commitRepository.existsByHash(repositoryId, hash)) {
                return hash;
            }
        }
        throw new SQLException("Unable to generate a unique merge commit hash");
    }

    private static String normalizeStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return Merge.STRATEGY_THREE_WAY;
        }
        for (String known : STRATEGIES) {
            if (known.equalsIgnoreCase(strategy.trim())) {
                return known;
            }
        }
        return strategy.trim();
    }

    private static String normalizeConflict(String conflictStatus) {
        if (conflictStatus == null || conflictStatus.isBlank()) {
            return Merge.CONFLICT_NONE;
        }
        String trimmed = conflictStatus.trim();
        if (Merge.CONFLICT_NONE.equalsIgnoreCase(trimmed)) {
            return Merge.CONFLICT_NONE;
        }
        if (Merge.CONFLICT_MINOR.equalsIgnoreCase(trimmed)) {
            return Merge.CONFLICT_MINOR;
        }
        if (Merge.CONFLICT_MAJOR.equalsIgnoreCase(trimmed)) {
            return Merge.CONFLICT_MAJOR;
        }
        return trimmed;
    }

    private static int simulateFilesChanged(String conflictStatus) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return switch (conflictStatus) {
            case Merge.CONFLICT_MINOR -> random.nextInt(2, 6);
            case Merge.CONFLICT_MAJOR -> random.nextInt(5, 12);
            default -> random.nextInt(1, 4);
        };
    }
}
