package com.gitforge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enriched commit payload used by the interactive commit graph.
 */
public class GraphCommitInfo {

    private final Commit commit;
    private final String branchName;
    private final boolean head;
    private final boolean mergeCommit;
    private final List<String> parentHashes;

    public GraphCommitInfo(Commit commit, String branchName, boolean head, boolean mergeCommit,
                           List<String> parentHashes) {
        this.commit = Objects.requireNonNull(commit, "commit");
        this.branchName = branchName == null ? "—" : branchName;
        this.head = head;
        this.mergeCommit = mergeCommit;
        this.parentHashes = parentHashes == null ? List.of() : List.copyOf(parentHashes);
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
        if (hash == null || hash.length() <= 7) {
            return hash == null ? "—" : hash;
        }
        return hash.substring(0, 7);
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

    public Long getBranchId() {
        return commit.getBranchId();
    }

    public String getBranchName() {
        return branchName;
    }

    public boolean isHead() {
        return head;
    }

    public boolean isMergeCommit() {
        return mergeCommit;
    }

    public List<String> getParentHashes() {
        return parentHashes;
    }

    public String getPrimaryParentHash() {
        return parentHashes.isEmpty() ? null : parentHashes.getFirst();
    }

    public String getSecondaryParentHash() {
        return parentHashes.size() > 1 ? parentHashes.get(1) : null;
    }

    public int getFilesChanged() {
        return commit.getFilesChanged();
    }

    public String getCommitType() {
        return commit.getCommitType();
    }

    public List<String> allParentHashes() {
        return new ArrayList<>(parentHashes);
    }
}
