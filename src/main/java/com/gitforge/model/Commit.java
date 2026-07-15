package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated commit record.
 */
public class Commit {

    private Long id;
    private Long repositoryId;
    private Long branchId;
    private String hash;
    private String message;
    private String author;
    private Instant committedAt;
    private String parentHash;

    public Commit() {
    }

    public Commit(Long repositoryId, Long branchId, String hash, String message,
                  String author, Instant committedAt, String parentHash) {
        this.repositoryId = repositoryId;
        this.branchId = branchId;
        this.hash = hash;
        this.message = message;
        this.author = author;
        this.committedAt = committedAt;
        this.parentHash = parentHash;
    }

    public Commit(Long id, Long repositoryId, Long branchId, String hash, String message,
                  String author, Instant committedAt, String parentHash) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.branchId = branchId;
        this.hash = hash;
        this.message = message;
        this.author = author;
        this.committedAt = committedAt;
        this.parentHash = parentHash;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Commit that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(hash, that.hash);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(repositoryId, hash);
    }

    @Override
    public String toString() {
        return "Commit{"
                + "id=" + id
                + ", repositoryId=" + repositoryId
                + ", branchId=" + branchId
                + ", hash='" + hash + '\''
                + ", message='" + message + '\''
                + ", author='" + author + '\''
                + ", committedAt=" + committedAt
                + ", parentHash='" + parentHash + '\''
                + '}';
    }
}
