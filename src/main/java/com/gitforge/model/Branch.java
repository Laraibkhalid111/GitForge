package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated branch record.
 */
public class Branch {

    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";

    private Long id;
    private Long repositoryId;
    private String name;
    private boolean active;
    private Instant createdAt;
    private Long parentBranchId;
    private String description;
    private String latestCommitHash;
    private String status;

    public Branch() {
    }

    public Branch(Long repositoryId, String name, boolean active) {
        this.repositoryId = repositoryId;
        this.name = name;
        this.active = active;
        this.status = active ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    public Branch(Long id, Long repositoryId, String name, boolean active, Instant createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
        this.status = active ? STATUS_ACTIVE : STATUS_INACTIVE;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? STATUS_ACTIVE : STATUS_INACTIVE;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getParentBranchId() {
        return parentBranchId;
    }

    public void setParentBranchId(Long parentBranchId) {
        this.parentBranchId = parentBranchId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLatestCommitHash() {
        return latestCommitHash;
    }

    public void setLatestCommitHash(String latestCommitHash) {
        this.latestCommitHash = latestCommitHash;
    }

    public String getStatus() {
        if (status == null || status.isBlank()) {
            return active ? STATUS_ACTIVE : STATUS_INACTIVE;
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if (STATUS_ACTIVE.equalsIgnoreCase(status)) {
            this.active = true;
        } else if (STATUS_INACTIVE.equalsIgnoreCase(status)) {
            this.active = false;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Branch that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(repositoryId, name);
    }

    @Override
    public String toString() {
        return name == null ? "Branch" : name;
    }
}
