package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.Branch;
import com.gitforge.model.Merge;
import com.gitforge.model.MergePreview;
import com.gitforge.model.MergeSummary;
import com.gitforge.model.RepositorySummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;
    private BranchService branchService;
    private CommitService commitService;
    private MergeService mergeService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("merge-module.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
        branchService = new BranchService();
        commitService = new CommitService();
        mergeService = new MergeService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void previewAndExecuteMergeUpdatesHistoryAndStats() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("MergeLab", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        commitService.createCommit(repo.getId(), main.getId(), "Base", "Dev", "Feature");

        BranchSummaryHelper feature = createFeature(repo.getId(), main);
        commitService.createCommit(repo.getId(), feature.id(), "Feature work", "Dev", "Feature");

        MergePreview preview = mergeService.previewMerge(
                repo.getId(), feature.id(), main.getId(), Merge.STRATEGY_THREE_WAY);
        assertEquals(feature.name(), preview.getSourceBranchName());
        assertEquals("main", preview.getTargetBranchName());
        assertNotNull(preview.getExpectedMergeCommit());
        assertNotNull(preview.getConflictStatus());

        MergeSummary result = mergeService.executeMerge(
                repo.getId(),
                feature.id(),
                main.getId(),
                Merge.STRATEGY_THREE_WAY,
                Merge.CONFLICT_NONE,
                true
        );

        assertEquals(Merge.STATUS_COMPLETED, result.getStatus());
        assertNotNull(result.getMergeCommitHash());
        assertEquals(1, mergeService.listHistory(repo.getId()).size());
        assertEquals(1, mergeService.countAllMerges());
        assertEquals(1, repositoryService.countAllMerges());
        assertTrue(repositoryService.countAllCommits() >= 3);

        Branch refreshedMain = branchService.listBranchesForRepository(repo.getId()).stream()
                .filter(b -> "main".equals(b.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(result.getMergeCommitHash(), refreshedMain.getLatestCommitHash());
    }

    @Test
    void simulationOnlyDoesNotUpdateTargetTip() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("SimOnly", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        commitService.createCommit(repo.getId(), main.getId(), "Base", "Dev", "Feature");
        main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        String tipBefore = main.getLatestCommitHash();

        BranchSummaryHelper feature = createFeature(repo.getId(), main);
        MergeSummary result = mergeService.executeMerge(
                repo.getId(),
                feature.id(),
                main.getId(),
                Merge.STRATEGY_SIMULATION_ONLY,
                Merge.CONFLICT_MINOR,
                true
        );

        assertEquals(Merge.STATUS_SIMULATED, result.getStatus());
        Branch refreshedMain = branchService.listBranchesForRepository(repo.getId()).stream()
                .filter(b -> "main".equals(b.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(tipBefore, refreshedMain.getLatestCommitHash());
        assertEquals(1, mergeService.listHistory(repo.getId()).size());
    }

    @Test
    void rejectsSameSourceAndTarget() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Same", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mergeService.previewMerge(repo.getId(), main.getId(), main.getId(), Merge.STRATEGY_FAST_FORWARD));
        assertTrue(ex.getMessage().toLowerCase().contains("different"));
    }

    @Test
    void cancelledMergeIsRejected() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("Cancel", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        BranchSummaryHelper feature = createFeature(repo.getId(), main);

        assertThrows(IllegalArgumentException.class,
                () -> mergeService.executeMerge(
                        repo.getId(), feature.id(), main.getId(),
                        Merge.STRATEGY_THREE_WAY, Merge.CONFLICT_NONE, false));
        assertEquals(0, mergeService.listHistory(repo.getId()).size());
    }

    private BranchSummaryHelper createFeature(long repositoryId, Branch main) throws Exception {
        var summary = branchService.createBranch(repositoryId, "feature/merge", main.getId(), "temp");
        return new BranchSummaryHelper(summary.getId(), summary.getName());
    }

    private record BranchSummaryHelper(Long id, String name) {
    }
}
