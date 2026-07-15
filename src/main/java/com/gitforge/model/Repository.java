package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated repository record.
 */
public class Repository {

    private Long id;
    private String name;
    private String path;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public Repository() {
    }

    public Repository(String name, String path, String description) {
        this.name = name;
        this.path = path;
        this.description = description;
    }

    public Repository(Long id, String name, String path, String description,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Repository that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name)
                && Objects.equals(path, that.path)
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(name, path, description);
    }

    @Override
    public String toString() {
        return "Repository{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", path='" + path + '\''
                + ", description='" + description + '\''
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
