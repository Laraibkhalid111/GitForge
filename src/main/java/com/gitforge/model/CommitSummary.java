package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Read model used by the Commit Management table and details panel.
 */
public class CommitSummary {

    private final Commit commit;
    private final String branchName;
    private final String repositoryName;

    public CommitSummary(Commit commit, String branchName, String repositoryName) {
        this.commit = Objects.requireNonNull(commit, "commit");
        this.branchName = branchName == null ? "—" : branchName;
        this.repositoryName = repositoryName == null ? "—" : repositoryName;
    }

    public Commit getCommit() {
        return commit;
    }

    public Long getId() {
        return commit.getId();
    }

    public String getHash() {
        return commit.getHash();
    }

    public String getShortHash() {
        String hash = commit.getHash();
        if (hash == null || hash.length() <= 8) {
            return hash == null ? "—" : hash;
        }
        return hash.substring(0, 8);
    }

    public String getMessage() {
        return commit.getMessage();
    }

    public String getAuthor() {
        return commit.getAuthor();
    }

    public Instant getCommittedAt() {
        return commit.getCommittedAt();
    }

    public String getParentHash() {
        return commit.getParentHash();
    }

    public String getCommitType() {
        return commit.getCommitType();
    }

    public int getFilesChanged() {
        return commit.getFilesChanged();
    }

    public String getBranchName() {
        return branchName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public Long getRepositoryId() {
        return commit.getRepositoryId();
    }

    public Long getBranchId() {
        return commit.getBranchId();
    }
}
