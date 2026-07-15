package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted application setting key/value pair.
 */
public class Settings {

    private Long id;
    private String key;
    private String value;
    private Instant updatedAt;

    public Settings() {
    }

    public Settings(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Settings(Long id, String key, String value, Instant updatedAt) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
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
        if (!(other instanceof Settings that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "Settings{"
                + "id=" + id
                + ", key='" + key + '\''
                + ", value='" + value + '\''
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
