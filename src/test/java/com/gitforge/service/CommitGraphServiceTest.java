package com.gitforge.service;

import com.gitforge.database.DatabaseManager;
import com.gitforge.model.Branch;
import com.gitforge.model.Merge;
import com.gitforge.model.RepositorySummary;
import com.gitforge.util.CommitGraph;
import com.gitforge.util.GraphEdge;
import com.gitforge.util.GraphNode;
import com.gitforge.model.GraphCommitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitGraphServiceTest {

    @TempDir
    Path tempDir;

    private DatabaseService databaseService;
    private RepositoryService repositoryService;
    private BranchService branchService;
    private CommitService commitService;
    private MergeService mergeService;
    private CommitGraphService graphService;

    @BeforeEach
    void setUp() throws Exception {
        Path databaseFile = tempDir.resolve("commit-graph.db");
        databaseService = new DatabaseService(DatabaseManager.getInstance());
        databaseService.initializeDatabase(databaseFile);
        repositoryService = new RepositoryService();
        branchService = new BranchService();
        commitService = new CommitService();
        mergeService = new MergeService();
        graphService = new CommitGraphService();
    }

    @AfterEach
    void tearDown() throws Exception {
        databaseService.shutdown();
    }

    @Test
    void buildsDagWithParentMergeEdgesAndTraversals() throws Exception {
        RepositorySummary repo = repositoryService.createRepository("GraphRepo", null, null, "main");
        Branch main = branchService.listBranchesForRepository(repo.getId()).getFirst();
        commitService.createCommit(repo.getId(), main.getId(), "Root", "Dev", "Feature");

        var feature = branchService.createBranch(repo.getId(), "feature/a", main.getId(), null);
        commitService.createCommit(repo.getId(), feature.getId(), "Feature tip", "Dev", "Feature");

        mergeService.executeMerge(
                repo.getId(),
                feature.getId(),
                main.getId(),
                Merge.STRATEGY_THREE_WAY,
                Merge.CONFLICT_NONE,
                true
        );

        CommitGraph<GraphCommitInfo> graph = graphService.buildGraph(repo.getId());
        assertFalse(graph.isEmpty());
        assertTrue(graph.size() >= 3);

        long mergeEdges = graph.getEdges().stream().filter(GraphEdge::isMergeEdge).count();
        assertTrue(mergeEdges >= 1);

        List<String> topo = graphService.topologicalOrder();
        assertEquals(graph.size(), topo.size());

        String first = topo.getFirst();
        List<String> dfs = graphService.traverseDfsFrom(first);
        List<String> bfs = graphService.traverseBfsFrom(first);
        assertFalse(dfs.isEmpty());
        assertFalse(bfs.isEmpty());

        GraphNode<GraphCommitInfo> headNode = graph.getNodes().stream()
                .filter(node -> node.getData().isHead())
                .findFirst()
                .orElseThrow();
        assertEquals(graphService.getHeadHash(), headNode.getId());
    }
}
