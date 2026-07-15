package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Read model for Merge History table and preview panels.
 */
public class MergeSummary {

    private final Merge merge;
    private final String sourceBranchName;
    private final String targetBranchName;
    private final String repositoryName;

    public MergeSummary(Merge merge, String sourceBranchName, String targetBranchName, String repositoryName) {
        this.merge = Objects.requireNonNull(merge, "merge");
        this.sourceBranchName = sourceBranchName == null ? "—" : sourceBranchName;
        this.targetBranchName = targetBranchName == null ? "—" : targetBranchName;
        this.repositoryName = repositoryName == null ? "—" : repositoryName;
    }

    public Merge getMerge() {
        return merge;
    }

    public Long getId() {
        return merge.getId();
    }

    public String getSourceBranchName() {
        return sourceBranchName;
    }

    public String getTargetBranchName() {
        return targetBranchName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getStrategy() {
        return merge.getStrategy() == null ? "—" : merge.getStrategy();
    }

    public String getStatus() {
        return merge.getStatus() == null ? "—" : merge.getStatus();
    }

    public Instant getMergedAt() {
        return merge.getMergedAt();
    }

    public String getMergeCommitHash() {
        return merge.getMergeCommitHash();
    }

    public String getShortMergeCommit() {
        String hash = merge.getMergeCommitHash();
        if (hash == null || hash.isBlank()) {
            return "—";
        }
        return hash.length() <= 8 ? hash : hash.substring(0, 8);
    }

    public String getConflictStatus() {
        return merge.getConflictStatus() == null ? "—" : merge.getConflictStatus();
    }
}
