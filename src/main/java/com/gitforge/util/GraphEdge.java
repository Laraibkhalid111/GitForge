package com.gitforge.util;

import java.util.Objects;

/**
 * A directed edge connecting two graph nodes.
 */
public class GraphEdge {

    public enum EdgeType {
        PARENT,
        MERGE
    }

    private final String fromId;
    private final String toId;
    private final EdgeType type;

    public GraphEdge(String fromId, String toId, EdgeType type) {
        this.fromId = Objects.requireNonNull(fromId, "fromId");
        this.toId = Objects.requireNonNull(toId, "toId");
        this.type = type == null ? EdgeType.PARENT : type;
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public EdgeType getType() {
        return type;
    }

    public boolean isMergeEdge() {
        return type == EdgeType.MERGE;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GraphEdge that)) {
            return false;
        }
        return fromId.equals(that.fromId)
                && toId.equals(that.toId)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, type);
    }

    @Override
    public String toString() {
        return fromId + " -> " + toId + " (" + type + ")";
    }
}
