package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.Branch;
import com.gitforge.model.CommitSummary;
import com.gitforge.model.RepositorySummary;
import com.gitforge.util.LinkedList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;
    private CommitService commitService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("commit-module.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
        commitService = new CommitService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void createCommitLinksParentAndUsesLinkedListHistory() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Alpha", null, null, "main");
        Branch branch = commitService.listBranchesForRepository(repo.getId()).getFirst();

        CommitSummary first = commitService.createCommit(
                repo.getId(), branch.getId(), "Initial commit", "Alice", "Feature");
        CommitSummary second = commitService.createCommit(
                repo.getId(), branch.getId(), "Second commit", "Alice", "Bug Fix");

        assertEquals(first.getHash(), second.getParentHash());
        assertEquals(2, commitService.displayCommitHistory().size());
        assertEquals(2, commitService.getCommitHistory().size());
        assertEquals(2, commitService.traverseHistory().size());
        assertEquals(2, repositoryService.findSummaryById(repo.getId()).orElseThrow().getTotalCommits());
    }

    @Test
    void deleteCommitRemovesFromDatabaseAndLinkedList() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Beta", null, null, "main");
        Branch branch = commitService.listBranchesForRepository(repo.getId()).getFirst();

        CommitSummary created = commitService.createCommit(
                repo.getId(), branch.getId(), "Remove me", "Bob", "Chore");

        assertTrue(commitService.deleteCommit(created.getId()));
        assertTrue(commitService.getCommitHistory().isEmpty());
        assertTrue(commitService.displayCommitHistory().isEmpty());
        assertEquals(0, repositoryService.findSummaryById(repo.getId()).orElseThrow().getTotalCommits());
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Gamma", null, null, "main");
        Branch branch = commitService.listBranchesForRepository(repo.getId()).getFirst();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> commitService.createCommit(repo.getId(), branch.getId(), "  ", "Dev", "Feature"));
        assertTrue(ex.getMessage().toLowerCase().contains("required"));
    }

    @Test
    void linkedListTracksInsertedCommits() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Linked", null, null, "main");
        Branch branch = commitService.listBranchesForRepository(repo.getId()).getFirst();

        CommitSummary created = commitService.createCommit(
                repo.getId(), branch.getId(), "Linked list commit", "Dev", "Feature");

        LinkedList<com.gitforge.model.Commit> history = commitService.getCommitHistory();
        assertEquals(1, history.size());
        assertTrue(history.find(c -> created.getId().equals(c.getId())).isPresent());
    }
}
