package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.Branch;
import com.gitforge.model.BranchSummary;
import com.gitforge.model.CommitSummary;
import com.gitforge.model.RepositorySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;
    private CommitService commitService;
    private BranchService branchService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("branch-module.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
        commitService = new CommitService();
        branchService = new BranchService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void createRenameSwitchDeleteAndTreeHierarchy() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Forest", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();

        CommitSummary commit = commitService.createCommit(
                repo.getId(), main.getId(), "Root work", "Dev", "Feature");

        BranchSummary feature = branchService.createBranch(
                repo.getId(), "feature/login", main.getId(), "Login work");
        assertEquals(main.getId(), feature.getParentBranchId());
        assertEquals(commit.getHash(), feature.getLatestCommitHash());
        assertEquals(Branch.STATUS_INACTIVE, feature.getStatus());

        List<BranchSummary> all = branchService.listSummaries(repo.getId());
        assertEquals(2, all.size());

        List<String> hierarchy = branchService.displayBranchHierarchy();
        assertTrue(hierarchy.getFirst().contains("main"));
        assertTrue(hierarchy.stream().anyMatch(line -> line.contains("feature/login")));

        BranchSummary renamed = branchService.renameBranch(feature.getId(), "feature/auth");
        assertEquals("feature/auth", renamed.getName());

        BranchSummary switched = branchService.switchBranch(repo.getId(), renamed.getId());
        assertTrue(switched.isActive());
        assertEquals(Branch.STATUS_ACTIVE, switched.getStatus());

        BranchSummary mainSummary = branchService.findSummaryById(main.getId()).orElseThrow();
        assertFalse(mainSummary.isActive());

        branchService.switchBranch(repo.getId(), main.getId());
        assertTrue(branchService.deleteBranch(renamed.getId()));
        assertEquals(1, branchService.listSummaries(repo.getId()).size());
    }

    @Test
    void rejectsDuplicateNamesAndBlankName() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Rules", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();

        branchService.createBranch(repo.getId(), "develop", main.getId(), null);

        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
                () -> branchService.createBranch(repo.getId(), "develop", main.getId(), null));
        assertTrue(duplicate.getMessage().toLowerCase().contains("already exists"));

        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> branchService.createBranch(repo.getId(), "  ", main.getId(), null));
        assertTrue(blank.getMessage().toLowerCase().contains("required"));
    }

    @Test
    void searchFindsByNameAndStatus() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("SearchRepo", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        branchService.createBranch(repo.getId(), "feature/search", main.getId(), null);

        assertEquals(1, branchService.searchSummaries(repo.getId(), "feature").size());
        assertEquals(1, branchService.searchSummaries(repo.getId(), "Active").size());
        assertEquals(2, branchService.searchSummaries(null, "SearchRepo").size());
    }
}
