package com.gitforge.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Persisted simulated merge record.
 */
public class Merge {

    private Long id;
    private Long repositoryId;
    private Long sourceBranchId;
    private Long targetBranchId;
    private String status;
    private String message;
    private Instant mergedAt;

    public Merge() {
    }

    public Merge(Long repositoryId, Long sourceBranchId, Long targetBranchId,
                 String status, String message, Instant mergedAt) {
        this.repositoryId = repositoryId;
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
        this.status = status;
        this.message = message;
        this.mergedAt = mergedAt;
    }

    public Merge(Long id, Long repositoryId, Long sourceBranchId, Long targetBranchId,
                 String status, String message, Instant mergedAt) {
        this.id = id;
        this.repositoryId = repositoryId;
        this.sourceBranchId = sourceBranchId;
        this.targetBranchId = targetBranchId;
        this.status = status;
        this.message = message;
        this.mergedAt = mergedAt;
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

    public Long getSourceBranchId() {
        return sourceBranchId;
    }

    public void setSourceBranchId(Long sourceBranchId) {
        this.sourceBranchId = sourceBranchId;
    }

    public Long getTargetBranchId() {
        return targetBranchId;
    }

    public void setTargetBranchId(Long targetBranchId) {
        this.targetBranchId = targetBranchId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(Instant mergedAt) {
        this.mergedAt = mergedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Merge that)) {
            return false;
        }
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(repositoryId, that.repositoryId)
                && Objects.equals(sourceBranchId, that.sourceBranchId)
                && Objects.equals(targetBranchId, that.targetBranchId)
                && Objects.equals(status, that.status)
                && Objects.equals(mergedAt, that.mergedAt);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(repositoryId, sourceBranchId, targetBranchId, status, mergedAt);
    }

    @Override
    public String toString() {
        return "Merge{"
                + "id=" + id
                + ", repositoryId=" + repositoryId
                + ", sourceBranchId=" + sourceBranchId
                + ", targetBranchId=" + targetBranchId
                + ", status='" + status + '\''
                + ", message='" + message + '\''
                + ", mergedAt=" + mergedAt
                + '}';
    }
}
