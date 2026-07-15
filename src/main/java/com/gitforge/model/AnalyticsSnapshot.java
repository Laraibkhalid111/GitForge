package com.gitforge.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable analytics snapshot rendered by the dashboard.
 */
public class AnalyticsSnapshot {

    private final int totalRepositories;
    private final int totalCommits;
    private final int totalBranches;
    private final int totalMerges;
    private final String activeBranch;
    private final String latestCommit;
    private final int healthScore;
    private final Map<String, Integer> commitsPerRepository;
    private final Map<String, Integer> branchDistribution;
    private final Map<String, Integer> commitActivityTimeline;
    private final Map<String, Integer> repositoryGrowth;
    private final Map<String, Integer> topContributors;
    private final List<RepositoryAnalytics> repositoryStats;
    private final List<ActivityItem> recentActivity;
    private final boolean fromCache;

    public AnalyticsSnapshot(int totalRepositories,
                             int totalCommits,
                             int totalBranches,
                             int totalMerges,
                             String activeBranch,
                             String latestCommit,
                             int healthScore,
                             Map<String, Integer> commitsPerRepository,
                             Map<String, Integer> branchDistribution,
                             Map<String, Integer> commitActivityTimeline,
                             Map<String, Integer> repositoryGrowth,
                             Map<String, Integer> topContributors,
                             List<RepositoryAnalytics> repositoryStats,
                             List<ActivityItem> recentActivity,
                             boolean fromCache) {
        this.totalRepositories = totalRepositories;
        this.totalCommits = totalCommits;
        this.totalBranches = totalBranches;
        this.totalMerges = totalMerges;
        this.activeBranch = activeBranch == null ? "—" : activeBranch;
        this.latestCommit = latestCommit == null ? "—" : latestCommit;
        this.healthScore = healthScore;
        this.commitsPerRepository = Map.copyOf(commitsPerRepository);
        this.branchDistribution = Map.copyOf(branchDistribution);
        this.commitActivityTimeline = Map.copyOf(commitActivityTimeline);
        this.repositoryGrowth = Map.copyOf(repositoryGrowth);
        this.topContributors = Map.copyOf(topContributors);
        this.repositoryStats = List.copyOf(repositoryStats);
        this.recentActivity = List.copyOf(recentActivity);
        this.fromCache = fromCache;
    }

    public int getTotalRepositories() {
        return totalRepositories;
    }

    public int getTotalCommits() {
        return totalCommits;
    }

    public int getTotalBranches() {
        return totalBranches;
    }

    public int getTotalMerges() {
        return totalMerges;
    }

    public String getActiveBranch() {
        return activeBranch;
    }

    public String getLatestCommit() {
        return latestCommit;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public Map<String, Integer> getCommitsPerRepository() {
        return commitsPerRepository;
    }

    public Map<String, Integer> getBranchDistribution() {
        return branchDistribution;
    }

    public Map<String, Integer> getCommitActivityTimeline() {
        return commitActivityTimeline;
    }

    public Map<String, Integer> getRepositoryGrowth() {
        return repositoryGrowth;
    }

    public Map<String, Integer> getTopContributors() {
        return topContributors;
    }

    public List<RepositoryAnalytics> getRepositoryStats() {
        return repositoryStats;
    }

    public List<ActivityItem> getRecentActivity() {
        return recentActivity;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public AnalyticsSnapshot withCacheFlag(boolean cached) {
        return new AnalyticsSnapshot(
                totalRepositories, totalCommits, totalBranches, totalMerges,
                activeBranch, latestCommit, healthScore,
                commitsPerRepository, branchDistribution, commitActivityTimeline,
                repositoryGrowth, topContributors, repositoryStats, recentActivity, cached
        );
    }

    /**
     * Filter criteria for analytics queries.
     */
    public static final class Filter {
        private Long repositoryId;
        private Long branchId;
        private String author;
        private LocalDate fromDate;
        private LocalDate toDate;

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

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }

        public String cacheKey() {
            return "repo=" + repositoryId
                    + "|branch=" + branchId
                    + "|author=" + (author == null ? "" : author.trim().toLowerCase())
                    + "|from=" + fromDate
                    + "|to=" + toDate;
        }

        public boolean isEmpty() {
            return repositoryId == null
                    && branchId == null
                    && (author == null || author.isBlank())
                    && fromDate == null
                    && toDate == null;
        }
    }

    /**
     * Per-repository statistics row.
     */
    public static final class RepositoryAnalytics {
        private final String name;
        private final String currentBranch;
        private final int branchCount;
        private final int commitCount;
        private final int mergeCount;
        private final String latestCommit;
        private final Instant createdAt;
        private final String simulatedSize;
        private final String status;

        public RepositoryAnalytics(String name, String currentBranch, int branchCount, int commitCount,
                                   int mergeCount, String latestCommit, Instant createdAt,
                                   String simulatedSize, String status) {
            this.name = name;
            this.currentBranch = currentBranch;
            this.branchCount = branchCount;
            this.commitCount = commitCount;
            this.mergeCount = mergeCount;
            this.latestCommit = latestCommit;
            this.createdAt = createdAt;
            this.simulatedSize = simulatedSize;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public String getCurrentBranch() {
            return currentBranch;
        }

        public int getBranchCount() {
            return branchCount;
        }

        public int getCommitCount() {
            return commitCount;
        }

        public int getMergeCount() {
            return mergeCount;
        }

        public String getLatestCommit() {
            return latestCommit;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public String getSimulatedSize() {
            return simulatedSize;
        }

        public String getStatus() {
            return status;
        }
    }

    /**
     * Recent activity feed entry.
     */
    public static final class ActivityItem {
        private final String type;
        private final String title;
        private final String detail;
        private final Instant timestamp;
        private final String iconLiteral;

        public ActivityItem(String type, String title, String detail, Instant timestamp, String iconLiteral) {
            this.type = Objects.requireNonNull(type, "type");
            this.title = title;
            this.detail = detail;
            this.timestamp = timestamp;
            this.iconLiteral = iconLiteral;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getDetail() {
            return detail;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public String getIconLiteral() {
            return iconLiteral;
        }
    }

    public static final class Builder {
        private int totalRepositories;
        private int totalCommits;
        private int totalBranches;
        private int totalMerges;
        private String activeBranch = "—";
        private String latestCommit = "—";
        private int healthScore;
        private Map<String, Integer> commitsPerRepository = new LinkedHashMap<>();
        private Map<String, Integer> branchDistribution = new LinkedHashMap<>();
        private Map<String, Integer> commitActivityTimeline = new LinkedHashMap<>();
        private Map<String, Integer> repositoryGrowth = new LinkedHashMap<>();
        private Map<String, Integer> topContributors = new LinkedHashMap<>();
        private List<RepositoryAnalytics> repositoryStats = new ArrayList<>();
        private List<ActivityItem> recentActivity = new ArrayList<>();
        private boolean fromCache;

        public Builder totalRepositories(int value) {
            this.totalRepositories = value;
            return this;
        }

        public Builder totalCommits(int value) {
            this.totalCommits = value;
            return this;
        }

        public Builder totalBranches(int value) {
            this.totalBranches = value;
            return this;
        }

        public Builder totalMerges(int value) {
            this.totalMerges = value;
            return this;
        }

        public Builder activeBranch(String value) {
            this.activeBranch = value;
            return this;
        }

        public Builder latestCommit(String value) {
            this.latestCommit = value;
            return this;
        }

        public Builder healthScore(int value) {
            this.healthScore = value;
            return this;
        }

        public Builder commitsPerRepository(Map<String, Integer> value) {
            this.commitsPerRepository = value;
            return this;
        }

        public Builder branchDistribution(Map<String, Integer> value) {
            this.branchDistribution = value;
            return this;
        }

        public Builder commitActivityTimeline(Map<String, Integer> value) {
            this.commitActivityTimeline = value;
            return this;
        }

        public Builder repositoryGrowth(Map<String, Integer> value) {
            this.repositoryGrowth = value;
            return this;
        }

        public Builder topContributors(Map<String, Integer> value) {
            this.topContributors = value;
            return this;
        }

        public Builder repositoryStats(List<RepositoryAnalytics> value) {
            this.repositoryStats = value;
            return this;
        }

        public Builder recentActivity(List<ActivityItem> value) {
            this.recentActivity = value;
            return this;
        }

        public Builder fromCache(boolean value) {
            this.fromCache = value;
            return this;
        }

        public AnalyticsSnapshot build() {
            return new AnalyticsSnapshot(
                    totalRepositories, totalCommits, totalBranches, totalMerges,
                    activeBranch, latestCommit, healthScore,
                    commitsPerRepository, branchDistribution, commitActivityTimeline,
                    repositoryGrowth, topContributors, repositoryStats, recentActivity, fromCache
            );
        }
    }
}
