package com.gitforge.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single vertex in a directed acyclic graph.
 *
 * @param <T> payload type stored in the node
 */
public class GraphNode<T> {

    private final String id;
    private T data;
    private double x;
    private double y;
    private final List<GraphEdge> outgoing = new ArrayList<>();
    private final List<GraphEdge> incoming = new ArrayList<>();

    public GraphNode(String id, T data) {
        this.id = Objects.requireNonNull(id, "id");
        this.data = Objects.requireNonNull(data, "data");
    }

    public String getId() {
        return id;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public List<GraphEdge> getOutgoing() {
        return Collections.unmodifiableList(outgoing);
    }

    public List<GraphEdge> getIncoming() {
        return Collections.unmodifiableList(incoming);
    }

    void addOutgoing(GraphEdge edge) {
        outgoing.add(edge);
    }

    void addIncoming(GraphEdge edge) {
        incoming.add(edge);
    }

    void removeOutgoing(GraphEdge edge) {
        outgoing.remove(edge);
    }

    void removeIncoming(GraphEdge edge) {
        incoming.remove(edge);
    }

    public int inDegree() {
        return incoming.size();
    }

    public int outDegree() {
        return outgoing.size();
    }
}
