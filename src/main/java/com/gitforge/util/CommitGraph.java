package com.gitforge.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Custom directed acyclic graph used to model simulated commit history.
 *
 * @param <T> node payload type
 */
public class CommitGraph<T> {

    private final Map<String, GraphNode<T>> nodes = new LinkedHashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();

    public int size() {
        return nodes.size();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    public void clear() {
        nodes.clear();
        edges.clear();
    }

    public GraphNode<T> addNode(String id, T data) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(data, "data");
        GraphNode<T> existing = nodes.get(id);
        if (existing != null) {
            existing.setData(data);
            return existing;
        }
        GraphNode<T> node = new GraphNode<>(id, data);
        nodes.put(id, node);
        return node;
    }

    public GraphEdge addEdge(String fromId, String toId, GraphEdge.EdgeType type) {
        if (!nodes.containsKey(fromId)) {
            throw new IllegalArgumentException("From node not found: " + fromId);
        }
        if (!nodes.containsKey(toId)) {
            throw new IllegalArgumentException("To node not found: " + toId);
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Self-edges are not allowed");
        }
        GraphEdge edge = new GraphEdge(fromId, toId, type);
        for (GraphEdge existing : edges) {
            if (existing.equals(edge)) {
                return existing;
            }
        }
        edges.add(edge);
        nodes.get(fromId).addOutgoing(edge);
        nodes.get(toId).addIncoming(edge);
        return edge;
    }

    public boolean removeNode(String id) {
        GraphNode<T> node = nodes.remove(id);
        if (node == null) {
            return false;
        }
        List<GraphEdge> related = new ArrayList<>();
        for (GraphEdge edge : edges) {
            if (edge.getFromId().equals(id) || edge.getToId().equals(id)) {
                related.add(edge);
            }
        }
        for (GraphEdge edge : related) {
            removeEdgeInternal(edge);
        }
        return true;
    }

    public Optional<GraphNode<T>> findNode(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    public List<GraphNode<T>> getNodes() {
        return List.copyOf(nodes.values());
    }

    public List<GraphEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    /**
     * Depth-first traversal starting from {@code startId}.
     */
    public List<String> traverseDfs(String startId) {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfsVisit(startId, visited, order::add);
        return order;
    }

    /**
     * Breadth-first traversal starting from {@code startId}.
     */
    public List<String> traverseBfs(String startId) {
        List<String> order = new ArrayList<>();
        if (!nodes.containsKey(startId)) {
            return order;
        }
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(startId);
        visited.add(startId);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            order.add(current);
            GraphNode<T> node = nodes.get(current);
            for (GraphEdge edge : node.getOutgoing()) {
                if (visited.add(edge.getToId())) {
                    queue.add(edge.getToId());
                }
            }
        }
        return order;
    }

    /**
     * Finds a path from {@code startId} to {@code goalId} using BFS.
     */
    public Optional<List<String>> findPath(String startId, String goalId) {
        if (!nodes.containsKey(startId) || !nodes.containsKey(goalId)) {
            return Optional.empty();
        }
        if (startId.equals(goalId)) {
            return Optional.of(List.of(startId));
        }

        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (GraphEdge edge : nodes.get(current).getOutgoing()) {
                String next = edge.getToId();
                if (visited.add(next)) {
                    parent.put(next, current);
                    if (next.equals(goalId)) {
                        return Optional.of(reconstructPath(parent, startId, goalId));
                    }
                    queue.add(next);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Simulated topological traversal (Kahn's algorithm).
     * Parents appear before their descendants when edges point parent → child.
     */
    public List<String> topologicalTraversal() {
        Map<String, Integer> remainingIn = new HashMap<>();
        for (String id : nodes.keySet()) {
            remainingIn.put(id, nodes.get(id).inDegree());
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : remainingIn.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            order.add(current);
            for (GraphEdge edge : nodes.get(current).getOutgoing()) {
                int nextIn = remainingIn.get(edge.getToId()) - 1;
                remainingIn.put(edge.getToId(), nextIn);
                if (nextIn == 0) {
                    queue.add(edge.getToId());
                }
            }
        }

        if (order.size() != nodes.size()) {
            // Cycle protection for malformed data — append remaining by insertion order.
            for (String id : nodes.keySet()) {
                if (!order.contains(id)) {
                    order.add(id);
                }
            }
        }
        return order;
    }

    public void forEachNode(Consumer<GraphNode<T>> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (GraphNode<T> node : nodes.values()) {
            consumer.accept(node);
        }
    }

    private void dfsVisit(String id, Set<String> visited, Consumer<String> visitor) {
        if (!nodes.containsKey(id) || !visited.add(id)) {
            return;
        }
        visitor.accept(id);
        for (GraphEdge edge : nodes.get(id).getOutgoing()) {
            dfsVisit(edge.getToId(), visited, visitor);
        }
    }

    private void removeEdgeInternal(GraphEdge edge) {
        edges.remove(edge);
        GraphNode<T> from = nodes.get(edge.getFromId());
        GraphNode<T> to = nodes.get(edge.getToId());
        if (from != null) {
            from.removeOutgoing(edge);
        }
        if (to != null) {
            to.removeIncoming(edge);
        }
    }

    private static List<String> reconstructPath(Map<String, String> parent, String startId, String goalId) {
        List<String> path = new ArrayList<>();
        String current = goalId;
        while (current != null) {
            path.add(current);
            if (current.equals(startId)) {
                break;
            }
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }
}
