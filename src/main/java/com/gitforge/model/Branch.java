package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated branch record.
 */
public class Branch {

    private Long id;
    private Long repositoryId;
    private String name;
    private boolean active;
    private Instant createdAt;

    public Branch() {
    }

    public Branch(Long repositoryId, String name, boolean active) {
        this.repositoryId = repositoryId;
        this.name = name;
        this.active = active;
    }

    public Branch(Long id, Long repositoryId, String name, boolean active, Instant createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.name = name;
        this.active = active;
        this.createdAt = createdAt;
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
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
        return "Branch{"
                + "id=" + id
                + ", repositoryId=" + repositoryId
                + ", name='" + name + '\''
                + ", active=" + active
                + ", createdAt=" + createdAt
                + '}';
    }
}
