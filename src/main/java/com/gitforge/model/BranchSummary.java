package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Read model used by the Branch Management table and details panel.
 */
public class BranchSummary {

    private final Branch branch;
    private final String repositoryName;
    private final String parentBranchName;
    private final int commitCount;

    public BranchSummary(Branch branch, String repositoryName, String parentBranchName, int commitCount) {
        this.branch = Objects.requireNonNull(branch, "branch");
        this.repositoryName = repositoryName == null ? "—" : repositoryName;
        this.parentBranchName = parentBranchName == null ? "—" : parentBranchName;
        this.commitCount = commitCount;
    }

    public Branch getBranch() {
        return branch;
    }

    public Long getId() {
        return branch.getId();
    }

    public String getName() {
        return branch.getName();
    }

    public Long getRepositoryId() {
        return branch.getRepositoryId();
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public Long getParentBranchId() {
        return branch.getParentBranchId();
    }

    public String getParentBranchName() {
        return parentBranchName;
    }

    public Instant getCreatedAt() {
        return branch.getCreatedAt();
    }

    public String getLatestCommitHash() {
        return branch.getLatestCommitHash();
    }

    public String getShortLatestCommit() {
        String hash = branch.getLatestCommitHash();
        if (hash == null || hash.isBlank()) {
            return "—";
        }
        return hash.length() <= 8 ? hash : hash.substring(0, 8);
    }

    public String getStatus() {
        return branch.getStatus();
    }

    public boolean isActive() {
        return branch.isActive();
    }

    public int getCommitCount() {
        return commitCount;
    }

    public String getDescription() {
        return branch.getDescription();
    }
}
