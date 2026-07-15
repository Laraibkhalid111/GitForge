package com.gitforge.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitGraphTest {

    private CommitGraph<String> graph;

    @BeforeEach
    void setUp() {
        graph = new CommitGraph<>();
    }

    @Test
    void addFindRemoveAndTraverse() {
        graph.addNode("a", "A");
        graph.addNode("b", "B");
        graph.addNode("c", "C");
        graph.addEdge("a", "b", GraphEdge.EdgeType.PARENT);
        graph.addEdge("b", "c", GraphEdge.EdgeType.PARENT);

        assertTrue(graph.findNode("b").isPresent());
        assertEquals(List.of("a", "b", "c"), graph.traverseDfs("a"));
        assertEquals(List.of("a", "b", "c"), graph.traverseBfs("a"));
        assertEquals(List.of("a", "b", "c"), graph.topologicalTraversal());

        assertTrue(graph.removeNode("b"));
        assertFalse(graph.findNode("b").isPresent());
        assertEquals(2, graph.size());
    }

    @Test
    void findPathAndMergeEdge() {
        graph.addNode("root", "root");
        graph.addNode("feature", "feature");
        graph.addNode("merge", "merge");
        graph.addEdge("root", "feature", GraphEdge.EdgeType.PARENT);
        graph.addEdge("root", "merge", GraphEdge.EdgeType.PARENT);
        graph.addEdge("feature", "merge", GraphEdge.EdgeType.MERGE);

        Optional<List<String>> path = graph.findPath("root", "merge");
        assertTrue(path.isPresent());
        assertEquals("root", path.get().getFirst());
        assertEquals("merge", path.get().getLast());

        assertEquals(1, graph.findNode("merge").orElseThrow().getIncoming().stream()
                .filter(GraphEdge::isMergeEdge)
                .count());
    }
}
