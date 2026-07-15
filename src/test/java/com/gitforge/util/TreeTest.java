package com.gitforge.util;

import com.gitforge.model.Branch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeTest {

    private Tree<Branch> tree;

    @BeforeEach
    void setUp() {
        tree = new Tree<>();
    }

    @Test
    void insertFindAndTraverse() {
        Branch main = branch(1L, "main", null);
        Branch feature = branch(2L, "feature", 1L);
        Branch hotfix = branch(3L, "hotfix", 1L);

        tree.setRoot(main);
        tree.insert(b -> b.getId() == 1L, feature);
        tree.insert(b -> b.getId() == 1L, hotfix);

        assertEquals(3, tree.size());
        assertTrue(tree.find(b -> "feature".equals(b.getName())).isPresent());

        List<String> names = new ArrayList<>();
        tree.traverse(branch -> names.add(branch.getName()));
        assertEquals(List.of("main", "feature", "hotfix"), names);
    }

    @Test
    void deleteReparentsChildren() {
        Branch main = branch(1L, "main", null);
        Branch feature = branch(2L, "feature", 1L);
        Branch nested = branch(3L, "nested", 2L);

        tree.setRoot(main);
        tree.insert(b -> b.getId() == 1L, feature);
        tree.insert(b -> b.getId() == 2L, nested);

        assertTrue(tree.delete(b -> b.getId() == 2L));
        assertEquals(2, tree.size());
        assertFalse(tree.find(b -> b.getId() == 2L).isPresent());
        assertTrue(tree.find(b -> b.getId() == 3L).isPresent());
        assertEquals(List.of("main", "  └─ nested"), tree.displayHierarchy(Branch::getName));
    }

    @Test
    void displayHierarchyShowsRootAndChildren() {
        tree.setRoot(branch(1L, "main", null));
        tree.insert(b -> b.getId() == 1L, branch(2L, "develop", 1L));

        assertEquals(List.of("main", "  └─ develop"), tree.displayHierarchy(Branch::getName));
    }

    private static Branch branch(long id, String name, Long parentId) {
        Branch branch = new Branch(id, 1L, name, false, null);
        branch.setParentBranchId(parentId);
        return branch;
    }
}
