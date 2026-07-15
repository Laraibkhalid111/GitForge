package com.gitforge.service;

import com.gitforge.model.AnalyticsSnapshot;
import com.gitforge.model.AnalyticsSnapshot.ActivityItem;
import com.gitforge.model.AnalyticsSnapshot.Filter;
import com.gitforge.model.AnalyticsSnapshot.RepositoryAnalytics;
import com.gitforge.model.Branch;
import com.gitforge.model.Commit;
import com.gitforge.model.Merge;
import com.gitforge.model.Repository;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.MergeRepository;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.util.AnalyticsCache;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aggregates repository analytics from existing DAO/repository layers.
 * Uses {@link AnalyticsCache} so the UI can avoid redundant SQLite reads.
 */
public class AnalyticsService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RepositoryRepository repositoryRepository;
    private final BranchRepository branchRepository;
    private final CommitRepository commitRepository;
    private final MergeRepository mergeRepository;
    private final AnalyticsCache<String, AnalyticsSnapshot> cache = new AnalyticsCache<>();

    public AnalyticsService() {
        this(new RepositoryRepository(), new BranchRepository(), new CommitRepository(), new MergeRepository());
    }

    public AnalyticsService(RepositoryRepository repositoryRepository,
                            BranchRepository branchRepository,
                            CommitRepository commitRepository,
                            MergeRepository mergeRepository) {
        this.repositoryRepository = repositoryRepository;
        this.branchRepository = branchRepository;
        this.commitRepository = commitRepository;
        this.mergeRepository = mergeRepository;
    }

    public AnalyticsCache<String, AnalyticsSnapshot> getCache() {
        return cache;
    }

    public void invalidateCache() {
        cache.clear();
    }

    /**
     * Returns a dashboard snapshot. Reads from cache when present; otherwise loads from SQLite.
     */
    public AnalyticsSnapshot loadDashboard(Filter filter, boolean forceRefresh) throws SQLException {
        Filter safe = filter == null ? new Filter() : filter;
        String key = safe.cacheKey();
        if (!forceRefresh) {
            var cached = cache.get(key);
            if (cached.isPresent()) {
                return cached.get().withCacheFlag(true);
            }
        }
        AnalyticsSnapshot snapshot = buildSnapshot(safe).withCacheFlag(false);
        cache.put(key, snapshot);
        return snapshot;
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public List<Branch> listBranches(Long repositoryId) throws SQLException {
        if (repositoryId == null) {
            return branchRepository.findAll();
        }
        return branchRepository.findByRepositoryId(repositoryId);
    }

    public List<String> listAuthors() throws SQLException {
        return commitRepository.findAll().stream()
                .map(Commit::getAuthor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(author -> !author.isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private AnalyticsSnapshot buildSnapshot(Filter filter) throws SQLException {
        List<Repository> repositories = repositoryRepository.findAll();
        List<Branch> branches = branchRepository.findAll();
        List<Commit> commits = commitRepository.findAll();
        List<Merge> merges = mergeRepository.findAll();

        if (filter.getRepositoryId() != null) {
            long repositoryId = filter.getRepositoryId();
            repositories = repositories.stream()
                    .filter(repo -> Objects.equals(repo.getId(), repositoryId))
                    .toList();
            branches = branches.stream()
                    .filter(branch -> Objects.equals(branch.getRepositoryId(), repositoryId))
                    .toList();
            commits = commits.stream()
                    .filter(commit -> Objects.equals(commit.getRepositoryId(), repositoryId))
                    .toList();
            merges = merges.stream()
                    .filter(merge -> Objects.equals(merge.getRepositoryId(), repositoryId))
                    .toList();
        }
        if (filter.getBranchId() != null) {
            long branchId = filter.getBranchId();
            branches = branches.stream()
                    .filter(branch -> Objects.equals(branch.getId(), branchId))
                    .toList();
            commits = commits.stream()
                    .filter(commit -> Objects.equals(commit.getBranchId(), branchId))
                    .toList();
        }
        if (filter.getAuthor() != null && !filter.getAuthor().isBlank()) {
            String authorNeedle = filter.getAuthor().trim().toLowerCase(Locale.ROOT);
            commits = commits.stream()
                    .filter(commit -> commit.getAuthor() != null
                            && commit.getAuthor().toLowerCase(Locale.ROOT).contains(authorNeedle))
                    .toList();
        }
        if (filter.getFromDate() != null || filter.getToDate() != null) {
            commits = commits.stream()
                    .filter(commit -> withinRange(commit.getCommittedAt(), filter.getFromDate(), filter.getToDate()))
                    .toList();
            merges = merges.stream()
                    .filter(merge -> withinRange(merge.getMergedAt(), filter.getFromDate(), filter.getToDate()))
                    .toList();
        }

        Map<Long, String> repositoryNames = repositories.stream()
                .collect(Collectors.toMap(Repository::getId, Repository::getName, (a, b) -> a, LinkedHashMap::new));

        Map<String, Integer> commitsPerRepository = new LinkedHashMap<>();
        for (Repository repository : repositories) {
            int count = (int) commits.stream()
                    .filter(commit -> Objects.equals(commit.getRepositoryId(), repository.getId()))
                    .count();
            commitsPerRepository.put(repository.getName(), count);
        }

        Map<String, Integer> branchDistribution = new LinkedHashMap<>();
        for (Branch branch : branches) {
            branchDistribution.merge(branch.getName(), 1, Integer::sum);
        }

        Map<String, Integer> timeline = buildCommitTimeline(commits);
        Map<String, Integer> growth = buildRepositoryGrowth(repositories);
        Map<String, Integer> contributors = buildTopContributors(commits);

        String activeBranch = branches.stream()
                .filter(Branch::isActive)
                .map(Branch::getName)
                .findFirst()
                .orElse(branches.isEmpty() ? "—" : branches.getFirst().getName());

        Commit latest = commits.stream()
                .max(Comparator.comparing(Commit::getCommittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        String latestCommit = latest == null
                ? "—"
                : shortHash(latest.getHash()) + " · " + nullToDash(latest.getMessage());

        List<RepositoryAnalytics> stats = buildRepositoryStats(repositories, branches, commits, merges);
        List<ActivityItem> activity = buildActivityFeed(repositories, branches, commits, merges, repositoryNames);

        int health = simulateHealthScore(repositories.size(), commits.size(), branches.size(), merges.size());

        return new AnalyticsSnapshot.Builder()
                .totalRepositories(repositories.size())
                .totalCommits(commits.size())
                .totalBranches(branches.size())
                .totalMerges(merges.size())
                .activeBranch(activeBranch)
                .latestCommit(latestCommit)
                .healthScore(health)
                .commitsPerRepository(commitsPerRepository)
                .branchDistribution(branchDistribution)
                .commitActivityTimeline(timeline)
                .repositoryGrowth(growth)
                .topContributors(contributors)
                .repositoryStats(stats)
                .recentActivity(activity)
                .build();
    }

    private List<RepositoryAnalytics> buildRepositoryStats(List<Repository> repositories,
                                                           List<Branch> branches,
                                                           List<Commit> commits,
                                                           List<Merge> merges) {
        List<RepositoryAnalytics> rows = new ArrayList<>();
        for (Repository repository : repositories) {
            List<Branch> repoBranches = branches.stream()
                    .filter(branch -> Objects.equals(branch.getRepositoryId(), repository.getId()))
                    .toList();
            List<Commit> repoCommits = commits.stream()
                    .filter(commit -> Objects.equals(commit.getRepositoryId(), repository.getId()))
                    .toList();
            int mergeCount = (int) merges.stream()
                    .filter(merge -> Objects.equals(merge.getRepositoryId(), repository.getId()))
                    .count();
            String currentBranch = repoBranches.stream()
                    .filter(Branch::isActive)
                    .map(Branch::getName)
                    .findFirst()
                    .orElse(repoBranches.isEmpty() ? "—" : repoBranches.getFirst().getName());
            Commit latest = repoCommits.stream()
                    .max(Comparator.comparing(Commit::getCommittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            String size = simulateRepositorySize(repoCommits.size(), repoBranches.size(), mergeCount);
            rows.add(new RepositoryAnalytics(
                    repository.getName(),
                    currentBranch,
                    repoBranches.size(),
                    repoCommits.size(),
                    mergeCount,
                    latest == null ? "—" : shortHash(latest.getHash()),
                    repository.getCreatedAt(),
                    size,
                    "Active"
            ));
        }
        rows.sort(Comparator.comparing(RepositoryAnalytics::getName, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private List<ActivityItem> buildActivityFeed(List<Repository> repositories,
                                                 List<Branch> branches,
                                                 List<Commit> commits,
                                                 List<Merge> merges,
                                                 Map<Long, String> repositoryNames) {
        List<ActivityItem> items = new ArrayList<>();
        for (Repository repository : repositories) {
            items.add(new ActivityItem(
                    "Repository Created",
                    "Repository \"" + repository.getName() + "\" created",
                    nullToDash(repository.getPath()),
                    repository.getCreatedAt(),
                    "mdi2s-source-repository"
            ));
        }
        for (Branch branch : branches) {
            String repoName = repositoryNames.getOrDefault(branch.getRepositoryId(), "repository");
            items.add(new ActivityItem(
                    "Branch Created",
                    "Branch \"" + branch.getName() + "\" created",
                    "in " + repoName,
                    branch.getCreatedAt(),
                    "mdi2s-source-branch"
            ));
        }
        for (Commit commit : commits) {
            String repoName = repositoryNames.getOrDefault(commit.getRepositoryId(), "repository");
            items.add(new ActivityItem(
                    "Commit Created",
                    shortHash(commit.getHash()) + " · " + nullToDash(commit.getMessage()),
                    nullToDash(commit.getAuthor()) + " · " + repoName,
                    commit.getCommittedAt(),
                    "mdi2s-source-commit"
            ));
        }
        for (Merge merge : merges) {
            String repoName = repositoryNames.getOrDefault(merge.getRepositoryId(), "repository");
            items.add(new ActivityItem(
                    "Merge Completed",
                    "Merge " + nullToDash(merge.getStatus()),
                    nullToDash(merge.getStrategy()) + " · " + repoName,
                    merge.getMergedAt(),
                    "mdi2s-source-merge"
            ));
        }
        items.sort(Comparator.comparing(ActivityItem::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (items.size() > 40) {
            return items.subList(0, 40);
        }
        return items;
    }

    private Map<String, Integer> buildCommitTimeline(List<Commit> commits) {
        Map<LocalDate, Integer> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 13; i >= 0; i--) {
            byDay.put(today.minusDays(i), 0);
        }
        for (Commit commit : commits) {
            if (commit.getCommittedAt() == null) {
                continue;
            }
            LocalDate day = LocalDate.ofInstant(commit.getCommittedAt(), ZoneId.systemDefault());
            if (byDay.containsKey(day)) {
                byDay.merge(day, 1, Integer::sum);
            }
        }
        Map<String, Integer> timeline = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Integer> entry : byDay.entrySet()) {
            timeline.put(DAY.format(entry.getKey()), entry.getValue());
        }
        return timeline;
    }

    private Map<String, Integer> buildRepositoryGrowth(List<Repository> repositories) {
        Map<String, Integer> growth = new LinkedHashMap<>();
        List<Repository> ordered = repositories.stream()
                .sorted(Comparator.comparing(Repository::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        int running = 0;
        for (Repository repository : ordered) {
            running++;
            String label = repository.getCreatedAt() == null
                    ? "n/a"
                    : MONTH.format(LocalDate.ofInstant(repository.getCreatedAt(), ZoneId.systemDefault()));
            growth.put(label + " #" + running, running);
        }
        if (growth.isEmpty()) {
            growth.put("Start", 0);
        }
        return growth;
    }

    private Map<String, Integer> buildTopContributors(List<Commit> commits) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Commit commit : commits) {
            String author = commit.getAuthor() == null || commit.getAuthor().isBlank()
                    ? "Unknown"
                    : commit.getAuthor().trim();
            counts.merge(author, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
                .limit(8)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static boolean withinRange(Instant instant, LocalDate from, LocalDate to) {
        if (instant == null) {
            return false;
        }
        LocalDate day = LocalDate.ofInstant(instant, ZoneId.systemDefault());
        if (from != null && day.isBefore(from)) {
            return false;
        }
        return to == null || !day.isAfter(to);
    }

    private static int simulateHealthScore(int repos, int commits, int branches, int merges) {
        if (repos == 0) {
            return 0;
        }
        int score = 40;
        score += Math.min(25, commits * 2);
        score += Math.min(15, branches * 3);
        score += Math.min(20, merges * 4);
        return Math.min(100, score);
    }

    private static String simulateRepositorySize(int commits, int branches, int merges) {
        int kb = Math.max(12, commits * 18 + branches * 8 + merges * 24);
        if (kb < 1024) {
            return kb + " KB";
        }
        return String.format(Locale.ROOT, "%.1f MB", kb / 1024.0);
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "—";
        }
        return hash.length() <= 7 ? hash : hash.substring(0, 7);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
