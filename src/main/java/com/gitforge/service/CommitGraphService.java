package com.gitforge.service;

import com.gitforge.model.Branch;
import com.gitforge.model.Commit;
import com.gitforge.model.GraphCommitInfo;
import com.gitforge.model.Merge;
import com.gitforge.model.Repository;
import com.gitforge.repository.BranchRepository;
import com.gitforge.repository.CommitRepository;
import com.gitforge.repository.MergeRepository;
import com.gitforge.repository.RepositoryRepository;
import com.gitforge.util.CommitGraph;
import com.gitforge.util.GraphEdge;
import com.gitforge.util.GraphNode;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds and lays out an interactive commit DAG from SQLite data.
 */
public class CommitGraphService {

    private static final double LANE_WIDTH = 168;
    private static final double ROW_HEIGHT = 112;
    private static final double ORIGIN_X = 96;
    private static final double ORIGIN_Y = 72;

    private final CommitRepository commitRepository;
    private final BranchRepository branchRepository;
    private final MergeRepository mergeRepository;
    private final RepositoryRepository repositoryRepository;

    private CommitGraph<GraphCommitInfo> graph = new CommitGraph<>();
    private final Map<String, String> branchColorHex = new LinkedHashMap<>();
    private String headHash;

    public CommitGraphService() {
        this(new CommitRepository(), new BranchRepository(), new MergeRepository(), new RepositoryRepository());
    }

    public CommitGraphService(CommitRepository commitRepository,
                              BranchRepository branchRepository,
                              MergeRepository mergeRepository,
                              RepositoryRepository repositoryRepository) {
        this.commitRepository = commitRepository;
        this.branchRepository = branchRepository;
        this.mergeRepository = mergeRepository;
        this.repositoryRepository = repositoryRepository;
    }

    public List<Repository> listRepositories() throws SQLException {
        return repositoryRepository.findAll();
    }

    public CommitGraph<GraphCommitInfo> getGraph() {
        return graph;
    }

    public String getHeadHash() {
        return headHash;
    }

    public Map<String, String> getBranchColorHex() {
        return Map.copyOf(branchColorHex);
    }

    /**
     * Loads commits, parent edges, and merge relationships for a repository into the DAG.
     */
    public CommitGraph<GraphCommitInfo> buildGraph(Long repositoryId) throws SQLException {
        graph = new CommitGraph<>();
        branchColorHex.clear();
        headHash = null;

        if (repositoryId == null) {
            return graph;
        }

        List<Commit> commits = commitRepository.findByRepositoryId(repositoryId);
        if (commits.isEmpty()) {
            return graph;
        }

        Map<Long, Branch> branches = new HashMap<>();
        for (Branch branch : branchRepository.findByRepositoryId(repositoryId)) {
            branches.put(branch.getId(), branch);
        }

        Map<String, Commit> byHash = new HashMap<>();
        for (Commit commit : commits) {
            byHash.put(commit.getHash(), commit);
        }

        Optional<Branch> active = branches.values().stream().filter(Branch::isActive).findFirst();
        if (active.isPresent()) {
            headHash = resolveLatestHash(active.get());
        }

        Map<String, List<String>> parentsByHash = new HashMap<>();
        for (Commit commit : commits) {
            List<String> parents = new ArrayList<>();
            if (commit.getParentHash() != null && !commit.getParentHash().isBlank()) {
                parents.add(commit.getParentHash());
            }
            parentsByHash.put(commit.getHash(), parents);
        }

        List<Merge> merges = mergeRepository.findByRepositoryId(repositoryId);
        for (Merge merge : merges) {
            if (merge.getMergeCommitHash() == null || merge.getMergeCommitHash().isBlank()) {
                continue;
            }
            String secondParent = resolveMergeSecondParent(merge, byHash);
            if (secondParent != null) {
                List<String> parents = parentsByHash.computeIfAbsent(merge.getMergeCommitHash(), key -> new ArrayList<>());
                if (!parents.contains(secondParent)) {
                    parents.add(secondParent);
                }
            }
        }

        int colorIndex = 0;
        for (Branch branch : branches.values().stream()
                .sorted(Comparator.comparing(Branch::getName, String.CASE_INSENSITIVE_ORDER))
                .toList()) {
            branchColorHex.put(branch.getName(), BranchPalette.hexAt(colorIndex++));
        }

        for (Commit commit : commits) {
            Branch branch = commit.getBranchId() == null ? null : branches.get(commit.getBranchId());
            String branchName = branch == null ? "—" : branch.getName();
            List<String> parents = parentsByHash.getOrDefault(commit.getHash(), List.of());
            boolean mergeCommit = parents.size() > 1
                    || "Merge".equalsIgnoreCase(commit.getCommitType());
            boolean isHead = Objects.equals(commit.getHash(), headHash);
            GraphCommitInfo info = new GraphCommitInfo(commit, branchName, isHead, mergeCommit, parents);
            graph.addNode(commit.getHash(), info);
        }

        for (Map.Entry<String, List<String>> entry : parentsByHash.entrySet()) {
            String childHash = entry.getKey();
            if (graph.findNode(childHash).isEmpty()) {
                continue;
            }
            List<String> parents = entry.getValue();
            for (int i = 0; i < parents.size(); i++) {
                String parentHash = parents.get(i);
                if (graph.findNode(parentHash).isEmpty()) {
                    continue;
                }
                GraphEdge.EdgeType type = i == 0 ? GraphEdge.EdgeType.PARENT : GraphEdge.EdgeType.MERGE;
                // Edge directed parent → child (time flows downward into descendants)
                graph.addEdge(parentHash, childHash, type);
            }
        }

        autoLayout();
        return graph;
    }

    /**
     * Positions nodes by topological layer (Y) and branch lane (X).
     */
    public void autoLayout() {
        if (graph.isEmpty()) {
            return;
        }

        List<String> topo = graph.topologicalTraversal();
        Map<String, Integer> laneByBranch = new LinkedHashMap<>();
        int nextLane = 0;
        for (String branchName : branchColorHex.keySet()) {
            laneByBranch.put(branchName, nextLane++);
        }

        Map<String, Integer> depth = new HashMap<>();
        for (String id : topo) {
            GraphNode<GraphCommitInfo> node = graph.findNode(id).orElseThrow();
            int maxParentDepth = -1;
            for (GraphEdge incoming : node.getIncoming()) {
                maxParentDepth = Math.max(maxParentDepth, depth.getOrDefault(incoming.getFromId(), 0));
            }
            int nodeDepth = maxParentDepth + 1;
            depth.put(id, nodeDepth);

            String branchName = node.getData().getBranchName();
            int lane = laneByBranch.computeIfAbsent(branchName, key -> {
                int assigned = laneByBranch.size();
                branchColorHex.putIfAbsent(key, BranchPalette.hexAt(assigned));
                return assigned;
            });

            node.setX(ORIGIN_X + lane * LANE_WIDTH);
            node.setY(ORIGIN_Y + nodeDepth * ROW_HEIGHT);
        }
    }

    public Optional<GraphCommitInfo> findCommit(String hash) {
        return graph.findNode(hash).map(GraphNode::getData);
    }

    public List<String> traverseDfsFrom(String hash) {
        return graph.traverseDfs(hash);
    }

    public List<String> traverseBfsFrom(String hash) {
        return graph.traverseBfs(hash);
    }

    public Optional<List<String>> findPath(String fromHash, String toHash) {
        return graph.findPath(fromHash, toHash);
    }

    public List<String> topologicalOrder() {
        return graph.topologicalTraversal();
    }

    private String resolveLatestHash(Branch branch) throws SQLException {
        if (branch.getLatestCommitHash() != null && !branch.getLatestCommitHash().isBlank()) {
            return branch.getLatestCommitHash();
        }
        return commitRepository.findLatestByBranchId(branch.getId())
                .map(Commit::getHash)
                .orElse(null);
    }

    private String resolveMergeSecondParent(Merge merge, Map<String, Commit> byHash) {
        if (merge.getSourceBranchId() == null) {
            return null;
        }
        Instant mergeTime = merge.getMergedAt();
        return byHash.values().stream()
                .filter(commit -> Objects.equals(commit.getBranchId(), merge.getSourceBranchId()))
                .filter(commit -> !Objects.equals(commit.getHash(), merge.getMergeCommitHash()))
                .filter(commit -> mergeTime == null
                        || commit.getCommittedAt() == null
                        || !commit.getCommittedAt().isAfter(mergeTime))
                .max(Comparator.comparing(Commit::getCommittedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Commit::getHash)
                .orElse(null);
    }

    /**
     * Distinct branch colors for graph lanes.
     */
    public static final class BranchPalette {

        private static final String[] COLORS = {
                "#3fb950", "#58a6ff", "#d29922", "#f85149",
                "#39d0d6", "#a371f7", "#ffa657", "#79c0ff"
        };

        private BranchPalette() {
        }

        public static String hexAt(int index) {
            return COLORS[Math.floorMod(index, COLORS.length)];
        }
    }
}
