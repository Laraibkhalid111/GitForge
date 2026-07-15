package com.gitforge.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Generic n-ary tree used by GitForge for in-memory branch hierarchy visualization.
 *
 * @param <T> element type stored in each node
 */
public class Tree<T> {

    /**
     * A single node in the tree.
     *
     * @param <T> element type
     */
    public static final class TreeNode<T> {

        private T data;
        private TreeNode<T> parent;
        private final List<TreeNode<T>> children = new ArrayList<>();

        TreeNode(T data) {
            this.data = Objects.requireNonNull(data, "data");
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = Objects.requireNonNull(data, "data");
        }

        public TreeNode<T> getParent() {
            return parent;
        }

        public List<TreeNode<T>> getChildren() {
            return List.copyOf(children);
        }

        public boolean isRoot() {
            return parent == null;
        }

        private void addChild(TreeNode<T> child) {
            children.add(child);
            child.parent = this;
        }

        private void removeChild(TreeNode<T> child) {
            children.remove(child);
            child.parent = null;
        }
    }

    private TreeNode<T> root;
    private int size;

    public TreeNode<T> getRoot() {
        return root;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * Sets or replaces the root node (for example the {@code main} branch).
     */
    public TreeNode<T> setRoot(T data) {
        Objects.requireNonNull(data, "data");
        root = new TreeNode<>(data);
        size = 1;
        return root;
    }

    /**
     * Inserts a child under the first node matching {@code parentPredicate}.
     * If the tree is empty, the inserted value becomes the root.
     */
    public TreeNode<T> insert(Predicate<T> parentPredicate, T data) {
        Objects.requireNonNull(data, "data");
        if (root == null) {
            return setRoot(data);
        }
        Objects.requireNonNull(parentPredicate, "parentPredicate");
        TreeNode<T> parent = findNode(parentPredicate)
                .orElseThrow(() -> new IllegalArgumentException("Parent node not found"));
        TreeNode<T> child = new TreeNode<>(data);
        parent.addChild(child);
        size++;
        return child;
    }

    /**
     * Inserts under the root when a parent is not specified.
     */
    public TreeNode<T> insert(T data) {
        if (root == null) {
            return setRoot(data);
        }
        TreeNode<T> child = new TreeNode<>(data);
        root.addChild(child);
        size++;
        return child;
    }

    /**
     * Removes the first matching node. Children are re-parented to the removed node's parent
     * (or become roots under a temporary forest by attaching to the tree root if the removed node is not root).
     * Deleting the root clears the tree when it has no children; otherwise the first child becomes the new root.
     */
    public boolean delete(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        Optional<TreeNode<T>> found = findNode(predicate);
        if (found.isEmpty()) {
            return false;
        }
        removeNode(found.get());
        return true;
    }

    public Optional<T> find(Predicate<T> predicate) {
        return findNode(predicate).map(TreeNode::getData);
    }

    public Optional<TreeNode<T>> findNode(Predicate<T> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        if (root == null) {
            return Optional.empty();
        }
        return findNodeRecursive(root, predicate);
    }

    /**
     * Depth-first pre-order traversal.
     */
    public void traverse(Consumer<T> consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (root == null) {
            return;
        }
        traverseRecursive(root, consumer);
    }

    /**
     * Returns indented lines representing the hierarchy for UI display.
     */
    public List<String> displayHierarchy(java.util.function.Function<T, String> labelFn) {
        Objects.requireNonNull(labelFn, "labelFn");
        List<String> lines = new ArrayList<>();
        if (root == null) {
            return lines;
        }
        displayRecursive(root, 0, labelFn, lines);
        return lines;
    }

    public List<T> toList() {
        List<T> values = new ArrayList<>(size);
        traverse(values::add);
        return values;
    }

    private void removeNode(TreeNode<T> node) {
        List<TreeNode<T>> children = new ArrayList<>(node.children);
        if (node == root) {
            if (children.isEmpty()) {
                clear();
                return;
            }
            TreeNode<T> newRoot = children.getFirst();
            node.removeChild(newRoot);
            for (int i = 1; i < children.size(); i++) {
                TreeNode<T> sibling = children.get(i);
                node.removeChild(sibling);
                newRoot.addChild(sibling);
            }
            root = newRoot;
            size--;
            return;
        }

        TreeNode<T> parent = node.parent;
        parent.removeChild(node);
        for (TreeNode<T> child : children) {
            node.removeChild(child);
            parent.addChild(child);
        }
        size--;
    }

    private Optional<TreeNode<T>> findNodeRecursive(TreeNode<T> node, Predicate<T> predicate) {
        if (predicate.test(node.data)) {
            return Optional.of(node);
        }
        for (TreeNode<T> child : node.children) {
            Optional<TreeNode<T>> match = findNodeRecursive(child, predicate);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    private void traverseRecursive(TreeNode<T> node, Consumer<T> consumer) {
        consumer.accept(node.data);
        for (TreeNode<T> child : node.children) {
            traverseRecursive(child, consumer);
        }
    }

    private void displayRecursive(TreeNode<T> node, int depth,
                                  java.util.function.Function<T, String> labelFn,
                                  List<String> lines) {
        String indent = "  ".repeat(depth);
        String prefix = depth == 0 ? "" : "└─ ";
        lines.add(indent + prefix + labelFn.apply(node.data));
        for (TreeNode<T> child : node.children) {
            displayRecursive(child, depth + 1, labelFn, lines);
        }
    }
}
