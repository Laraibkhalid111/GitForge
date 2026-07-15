package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.AnalyticsSnapshot;
import com.gitforge.model.AnalyticsSnapshot.Filter;
import com.gitforge.model.Branch;
import com.gitforge.model.RepositorySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;
    private BranchService branchService;
    private CommitService commitService;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("analytics.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
        branchService = new BranchService();
        commitService = new CommitService();
        analyticsService = new AnalyticsService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void loadsDashboardAndUsesCache() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Stats", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        commitService.createCommit(repo.getId(), main.getId(), "Work", "Alice", "Feature");

        AnalyticsSnapshot first = analyticsService.loadDashboard(new Filter(), false);
        assertFalse(first.isFromCache());
        assertEquals(1, first.getTotalRepositories());
        assertEquals(1, first.getTotalCommits());
        assertTrue(first.getHealthScore() > 0);
        assertFalse(first.getCommitsPerRepository().isEmpty());
        assertFalse(first.getRecentActivity().isEmpty());

        AnalyticsSnapshot second = analyticsService.loadDashboard(new Filter(), false);
        assertTrue(second.isFromCache());
        assertEquals(1, analyticsService.getCache().size());

        analyticsService.invalidateCache();
        AnalyticsSnapshot third = analyticsService.loadDashboard(new Filter(), true);
        assertFalse(third.isFromCache());
    }
}
