package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Read model used by the Repository Management table and details panel.
 * Aggregates persisted repository data with branch/commit counts.
 */
public class RepositorySummary {

    public static final String STATUS_ACTIVE = "Active";

    private final Repository repository;
    private final String currentBranch;
    private final int totalBranches;
    private final int totalCommits;
    private final String status;

    public RepositorySummary(Repository repository, String currentBranch,
                             int totalBranches, int totalCommits, String status) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.currentBranch = currentBranch;
        this.totalBranches = totalBranches;
        this.totalCommits = totalCommits;
        this.status = status == null ? STATUS_ACTIVE : status;
    }

    public Repository getRepository() {
        return repository;
    }

    public Long getId() {
        return repository.getId();
    }

    public String getName() {
        return repository.getName();
    }

    public String getDescription() {
        return repository.getDescription();
    }

    public String getPath() {
        return repository.getPath();
    }

    public Instant getCreatedAt() {
        return repository.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return repository.getUpdatedAt();
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public int getTotalBranches() {
        return totalBranches;
    }

    public int getTotalCommits() {
        return totalCommits;
    }

    public String getStatus() {
        return status;
    }
}
