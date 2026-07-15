package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.RepositorySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("repo-module.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void createReadUpdateDeleteAndSearch() throws Exception {
        RepositorySummary created = repositoryService.createRepository(
                "Alpha", "First repo", null, "main");
        assertEquals("Alpha", created.getName());
        assertEquals("main", created.getCurrentBranch());
        assertEquals(1, created.getTotalBranches());
        assertEquals(0, created.getTotalCommits());
        assertEquals(RepositorySummary.STATUS_ACTIVE, created.getStatus());

        List<RepositorySummary> all = repositoryService.listSummaries();
        assertEquals(1, all.size());

        RepositorySummary updated = repositoryService.updateRepository(
                created.getId(), "Alpha Two", "Updated", "C:/simulated/alpha");
        assertEquals("Alpha Two", updated.getName());
        assertEquals("Updated", updated.getDescription());
        assertEquals("C:/simulated/alpha", updated.getPath());

        List<RepositorySummary> searchHits = repositoryService.searchSummaries("Alpha");
        assertEquals(1, searchHits.size());

        assertTrue(repositoryService.deleteRepository(created.getId()));
        assertTrue(repositoryService.listSummaries().isEmpty());
    }

    @Test
    void rejectsDuplicateNames() throws Exception {
        repositoryService.createRepository("Demo", null, null, "main");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> repositoryService.createRepository("demo", null, null, "main"));
        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
    }

    @Test
    void rejectsBlankName() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> repositoryService.createRepository("  ", null, null, "main"));
        assertTrue(ex.getMessage().toLowerCase().contains("required"));
    }
}
