package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated stash entry.
 */
public class Stash {

    private Long id;
    private Long repositoryId;
    private String name;
    private String message;
    private Instant createdAt;

    public Stash() {
    }

    public Stash(Long repositoryId, String name, String message) {
        this.repositoryId = repositoryId;
        this.name = name;
        this.message = message;
    }

    public Stash(Long id, Long repositoryId, String name, String message, Instant createdAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.name = name;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
        if (!(other instanceof Stash that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(name, that.name)
                && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(repositoryId, name, message);
    }

    @Override
    public String toString() {
        return "Stash{"
                + "id=" + id
                + ", repositoryId=" + repositoryId
                + ", name='" + name + '\''
                + ", message='" + message + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
