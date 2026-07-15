package com.gitforge.service;

import com.gitforge.database.ConnectionManager;
import com.gitforge.model.Branch;
import com.gitforge.model.BranchSummary;
import com.gitforge.model.Commit;
import com.gitforge.model.Repository;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.util.Tree;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service for simulated branch management.
 * Branch hierarchy is held in a custom {@link Tree} while SQLite remains persistent storage.
 */
public class BranchService {

    public static final String DEFAULT_BRANCH = "main";

    private final BranchRepository branchRepository;
    private final RepositoryRepository repositoryRepository;
    private final CommitRepository commitRepository;
    private final Tree<Branch> branchTree = new Tree<>();

    public BranchService() {
        this(new BranchRepository(), new RepositoryRepository(), new CommitRepository());
    }

    public BranchService(BranchRepository branchRepository,
                         RepositoryRepository repositoryRepository,
                         CommitRepository commitRepository) {
        this.branchRepository = branchRepository;
        this.repositoryRepository = repositoryRepository;
        this.commitRepository = commitRepository;
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public List<Branch> listBranchesForRepository(long repositoryId) throws SQLException {
        return branchRepository.findByRepositoryId(repositoryId);
    }

    /**
     * Reloads SQLite branches for the filter into the in-memory tree and returns table summaries.
     */
    public List<BranchSummary> searchSummaries(Long repositoryId, String query) throws SQLException {
        List<Branch> branches = loadBranches(repositoryId, query);
        rebuildTree(repositoryId);
        return toSummaries(branches);
    }

    public List<BranchSummary> listSummaries(Long repositoryId) throws SQLException {
        return searchSummaries(repositoryId, null);
    }

    public Optional<BranchSummary> findSummaryById(long id) throws SQLException {
        Optional<Branch> branch = branchRepository.findById(id);
        if (branch.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toSummary(branch.get()));
    }

    public Tree<Branch> getBranchTree() {
        return branchTree;
    }

    public List<String> displayBranchHierarchy() {
        return branchTree.displayHierarchy(Branch::getName);
    }

    public Optional<Branch> findBranch(long id) {
        return branchTree.find(candidate -> candidate.getId() != null && candidate.getId() == id);
    }

    public List<Branch> traverseTree() {
        return branchTree.toList();
    }

    public BranchSummary createBranch(long repositoryId, String name, Long parentBranchId, String description)
            throws SQLException {
        repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        String normalizedName = requireUniqueName(repositoryId, name, null);
        Branch parent = resolveParent(repositoryId, parentBranchId);
        String latestCommit = parent == null ? null : parent.getLatestCommitHash();
        if (parent != null && (latestCommit == null || latestCommit.isBlank())) {
            latestCommit = commitRepository.findLatestByBranchId(parent.getId())
                    .map(Commit::getHash)
                    .orElse(null);
        }

        Branch branch = new Branch(repositoryId, normalizedName, false);
        branch.setParentBranchId(parent == null ? null : parent.getId());
        branch.setDescription(trimToNull(description));
        branch.setLatestCommitHash(latestCommit);
        branch.setStatus(Branch.STATUS_INACTIVE);

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            branchRepository.create(branch);
            connection.commit();
            Branch persisted = branchRepository.findById(branch.getId()).orElseThrow();
            insertBranchIntoTree(persisted);
            return toSummary(persisted);
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to create branch", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public BranchSummary renameBranch(long id, String newName) throws SQLException {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        String normalized = requireUniqueName(existing.getRepositoryId(), newName, id);
        existing.setName(normalized);
        if (!branchRepository.update(existing)) {
            throw new SQLException("Branch rename failed");
        }
        Branch persisted = branchRepository.findById(id).orElseThrow();
        branchTree.findNode(b -> Objects.equals(b.getId(), id))
                .ifPresent(node -> node.setData(persisted));
        return toSummary(persisted);
    }

    public boolean deleteBranch(long id) throws SQLException {
        Branch existing = branchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

        int branchCount = branchRepository.countByRepositoryId(existing.getRepositoryId());
        if (branchCount <= 1) {
            throw new IllegalArgumentException("Cannot delete the only branch in a repository");
        }
        if (existing.isActive()) {
            throw new IllegalArgumentException("Cannot delete the active branch. Switch to another branch first.");
        }
        if (DEFAULT_BRANCH.equalsIgnoreCase(existing.getName()) && existing.getParentBranchId() == null) {
            throw new IllegalArgumentException("Cannot delete the root main branch");
        }

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            branchRepository.reparentChildren(id, existing.getParentBranchId());
            boolean deleted = branchRepository.delete(id);
            connection.commit();
            if (deleted) {
                deleteBranchFromTree(id);
            }
            return deleted;
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to delete branch", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public BranchSummary switchBranch(long repositoryId, long branchId) throws SQLException {
        Branch target = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        if (target.getRepositoryId() == null || target.getRepositoryId() != repositoryId) {
            throw new IllegalArgumentException("Branch does not belong to the selected repository");
        }

        Connection connection = ConnectionManager.getInstance().getConnection();
        boolean previous = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            branchRepository.deactivateAllInRepository(repositoryId);
            target.setActive(true);
            target.setStatus(Branch.STATUS_ACTIVE);
            branchRepository.update(target);
            connection.commit();
            return toSummary(branchRepository.findById(branchId).orElseThrow());
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            throw new SQLException("Unable to switch branch", ex);
        } finally {
            connection.setAutoCommit(previous);
        }
    }

    public void insertBranchIntoTree(Branch branch) {
        Objects.requireNonNull(branch, "branch");
        if (branchTree.isEmpty() || branch.getParentBranchId() == null) {
            if (branchTree.isEmpty()) {
                branchTree.setRoot(branch);
            } else if (branch.getParentBranchId() == null) {
                branchTree.insert(branch);
            } else {
                boolean inserted = tryInsertUnderParent(branch);
                if (!inserted) {
                    branchTree.insert(branch);
                }
            }
            return;
        }
        if (!tryInsertUnderParent(branch)) {
            branchTree.insert(branch);
        }
    }

    public boolean deleteBranchFromTree(long id) {
        return branchTree.delete(candidate -> candidate.getId() != null && candidate.getId() == id);
    }

    private boolean tryInsertUnderParent(Branch branch) {
        Optional<Tree.TreeNode<Branch>> parentNode = branchTree.findNode(
                candidate -> Objects.equals(candidate.getId(), branch.getParentBranchId()));
        if (parentNode.isEmpty()) {
            return false;
        }
        branchTree.insert(b -> Objects.equals(b.getId(), branch.getParentBranchId()), branch);
        return true;
    }

    private void rebuildTree(Long repositoryId) throws SQLException {
        branchTree.clear();
        if (repositoryId == null) {
            return;
        }
        List<Branch> allInRepo = branchRepository.findByRepositoryId(repositoryId);
        buildTreeForRepository(allInRepo);
    }

    private void buildTreeForRepository(List<Branch> branches) {
        if (branches.isEmpty()) {
            return;
        }

        Branch root = branches.stream()
                .filter(b -> DEFAULT_BRANCH.equalsIgnoreCase(b.getName()) && b.getParentBranchId() == null)
                .findFirst()
                .orElseGet(() -> branches.stream()
                        .filter(b -> b.getParentBranchId() == null)
                        .min(Comparator.comparing(Branch::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElse(branches.getFirst()));

        branchTree.setRoot(root);

        List<Branch> remaining = new ArrayList<>(branches);
        remaining.removeIf(b -> Objects.equals(b.getId(), root.getId()));
        remaining.sort(Comparator.comparing(Branch::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        boolean progress = true;
        while (!remaining.isEmpty() && progress) {
            progress = false;
            List<Branch> stillPending = new ArrayList<>();
            for (Branch branch : remaining) {
                if (branch.getParentBranchId() != null
                        && branchTree.find(b -> Objects.equals(b.getId(), branch.getParentBranchId())).isPresent()) {
                    branchTree.insert(b -> Objects.equals(b.getId(), branch.getParentBranchId()), branch);
                    progress = true;
                } else if (branch.getParentBranchId() == null) {
                    branchTree.insert(branch);
                    progress = true;
                } else {
                    stillPending.add(branch);
                }
            }
            remaining = stillPending;
        }
        for (Branch orphan : remaining) {
            branchTree.insert(orphan);
        }
    }

    private List<Branch> loadBranches(Long repositoryId, String query) throws SQLException {
        if (query == null || query.isBlank()) {
            return repositoryId == null
                    ? branchRepository.findAll()
                    : branchRepository.findByRepositoryId(repositoryId);
        }
        String trimmed = query.trim();
        List<Branch> branches = repositoryId == null
                ? branchRepository.search(trimmed)
                : branchRepository.searchByRepository(repositoryId, trimmed);

        if (repositoryId == null) {
            return filterByRepositoryName(branches, trimmed);
        }
        return branches;
    }

    private List<Branch> filterByRepositoryName(List<Branch> branches, String query) throws SQLException {
        String needle = query.toLowerCase(Locale.ROOT);
        Map<Long, String> repositoryNames = repositoryRepository.findAll().stream()
                .collect(Collectors.toMap(Repository::getId, Repository::getName, (a, b) -> a));

        List<Branch> fromSearch = new ArrayList<>(branches);
        for (Branch branch : branchRepository.findAll()) {
            if (fromSearch.stream().anyMatch(b -> Objects.equals(b.getId(), branch.getId()))) {
                continue;
            }
            String repoName = repositoryNames.getOrDefault(branch.getRepositoryId(), "");
            if (repoName.toLowerCase(Locale.ROOT).contains(needle)) {
                fromSearch.add(branch);
            }
        }
        fromSearch.sort(Comparator.comparing(Branch::getName, String.CASE_INSENSITIVE_ORDER));
        return fromSearch;
    }

    private List<BranchSummary> toSummaries(List<Branch> branches) throws SQLException {
        List<BranchSummary> summaries = new ArrayList<>(branches.size());
        for (Branch branch : branches) {
            summaries.add(toSummary(branch));
        }
        return summaries;
    }

    private BranchSummary toSummary(Branch branch) throws SQLException {
        String repositoryName = repositoryRepository.findById(branch.getRepositoryId())
                .map(Repository::getName)
                .orElse("—");
        String parentName = "—";
        if (branch.getParentBranchId() != null) {
            parentName = branchRepository.findById(branch.getParentBranchId())
                    .map(Branch::getName)
                    .orElse("—");
        }
        int commitCount = commitRepository.countByBranchId(branch.getId());
        return new BranchSummary(branch, repositoryName, parentName, commitCount);
    }

    private Branch resolveParent(long repositoryId, Long parentBranchId) throws SQLException {
        if (parentBranchId != null) {
            Branch parent = branchRepository.findById(parentBranchId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent branch not found"));
            if (parent.getRepositoryId() == null || parent.getRepositoryId() != repositoryId) {
                throw new IllegalArgumentException("Parent branch does not belong to the selected repository");
            }
            return parent;
        }
        Optional<Branch> main = branchRepository.findByRepositoryId(repositoryId).stream()
                .filter(b -> DEFAULT_BRANCH.equalsIgnoreCase(b.getName()))
                .findFirst();
        if (main.isPresent()) {
            return main.get();
        }
        return branchRepository.findActiveByRepositoryId(repositoryId).orElse(null);
    }

    private String requireUniqueName(long repositoryId, String name, Long excludeId) throws SQLException {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Branch name is required");
        }
        String normalized = name.trim();
        if (branchRepository.existsByNameIgnoreCase(repositoryId, normalized, excludeId)) {
            throw new IllegalArgumentException("A branch named \"" + normalized + "\" already exists in this repository");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
